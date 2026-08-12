import json
import sys
import base64
import requests
import os
from Crypto.Cipher import AES

# --- Chaquopy Environment Patch ---
class FakeAsyncio:
    def __init__(self):
        self.__name__ = "asyncio"
        self.iscoroutinefunction = lambda obj: False
        self.iscoroutine = lambda obj: False
    def coroutine(self, f): return f
    def sleep(self, delay): import time; time.sleep(delay)
    def get_event_loop(self): return self
    def __getattr__(self, name): return lambda *args, **kwargs: None
sys.modules["asyncio"] = FakeAsyncio()

# --- MEGA Decryption Utilities ---
def base64_url_decode(data):
    data += '=' * ((4 - len(data) % 4) % 4)
    return base64.urlsafe_b64decode(data)

def aes_ecb_decrypt(data, key):
    return AES.new(key, AES.MODE_ECB).decrypt(data)

class MegaManager:
    def __init__(self):
        from mega import Mega
        self.mega = Mega()
        self.current_folder_id = None
        self.session = requests.Session() 

    def decrypt_attr(self, attr_data, key):
        try:
            attr_data = base64_url_decode(attr_data)
            cipher = AES.new(key, AES.MODE_CBC, b'\x00' * 16)
            decrypted = cipher.decrypt(attr_data)
            if decrypted.startswith(b'MEGA'):
                res = decrypted[4:].split(b'\0', 1)[0].decode('utf-8')
                return json.loads(res)
        except: pass
        return {}

    def list_shared_folder(self, folder_url):
        try:
            if "/folder/" not in folder_url: return {"status": "error"}
            parts = folder_url.split("/folder/")[1].split("#")
            folder_id = parts[0]
            folder_key = base64_url_decode(parts[1] if len(parts) > 1 else "")
            self.current_folder_id = folder_id
            
            api_url = f"https://g.api.mega.co.nz/cs?id=100&n={folder_id}"
            r = self.session.post(api_url, json=[{"a": "f", "c": 1, "r": 1}], timeout=15).json()
            if not r or "f" not in r[0]: return {"status": "error"}
            
            node_list = []
            img_exts = ('.png', '.jpg', '.jpeg', '.gif', '.bmp', '.webp')
            for node in r[0]["f"]:
                if node.get("t") == 0:
                    try:
                        k = node.get("k")
                        if ':' in k: k = k.split(':', 1)[1]
                        enc_k = base64_url_decode(k)
                        dec_k = b''
                        for i in range(0, len(enc_k), 16): 
                            dec_k += aes_ecb_decrypt(enc_k[i:i+16], folder_key)
                        
                        attr_key = bytes([dec_k[i] ^ dec_k[i+16] for i in range(16)]) if len(dec_k) == 32 else dec_k[:16]
                        attrs = self.decrypt_attr(node.get("a"), attr_key)
                        name = attrs.get("n", "Unknown")

                        if any(name.lower().endswith(ext) for ext in img_exts):
                            b64_k = base64.urlsafe_b64encode(dec_k).decode('utf-8').rstrip('=')
                            node_list.append({"h": f"{node['h']}#{b64_k}", "name": name, "type": "image"})
                    except: continue
            return {"status": "success", "nodes": node_list}
        except: return {"status": "error"}

    def download_file(self, id_key, dest_path, force_filename=None):
        try:
            if not os.path.exists(dest_path):
                os.makedirs(dest_path, exist_ok=True)

            if "mega.nz/file/" in id_key:
                # FIX: Use explicit filename if provided, otherwise let Mega library decide
                self.mega.download_url(id_key, dest_path=dest_path, dest_filename=force_filename)
                return {"status": "success"}

            # Standard individual file download logic
            h, k_b64 = id_key.split("#")
            node_key = base64_url_decode(k_b64)
            
            api_url = f"https://g.api.mega.co.nz/cs?id=200"
            if self.current_folder_id: api_url += f"&n={self.current_folder_id}"
            
            payload = [{"a": "g", "g": 1, "n": h}]
            resp_raw = self.session.post(api_url, json=payload, timeout=15).json()
            try: resp = resp_raw[0]
            except: resp = resp_raw
            
            if 'g' not in resp: return {"status": "error"}
            
            file_url = resp['g']
            key_bytes = bytes([node_key[i] ^ node_key[i+16] for i in range(16)])
            iv = node_key[16:24] + b'\x00' * 8
            iv_val = 0
            for b in iv: iv_val = (iv_val << 8) | b
            from Crypto.Util import Counter
            decryptor = AES.new(key_bytes, AES.MODE_CTR, counter=Counter.new(128, initial_value=iv_val))
            
            r = self.session.get(file_url, stream=True, timeout=20)
            r.raise_for_status()
            
            out_file = os.path.join(dest_path, force_filename if force_filename else f"dl_{h}.jpg")
            with open(out_file, 'wb') as f:
                for chunk in r.iter_content(chunk_size=64*1024):
                    if chunk:
                        f.write(decryptor.decrypt(chunk))
            
            return {"status": "success"}
        except: return {"status": "error"}

manager = MegaManager()
def list_shared_folder(u): return json.dumps(manager.list_shared_folder(u))
def download_file(u, d, f=None): return json.dumps(manager.download_file(u, d, force_filename=f))
