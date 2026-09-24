import subprocess
import re
import time
import os

def run_cloudflare_tunnel():
    exe_path = r"C:\Users\User\cloudflared.exe"
    cmd = [exe_path, "tunnel", "--url", "http://localhost:3000"]

    print("Starting Cloudflare Quick Tunnel for StockPilot Web...")

    while True:
        try:
            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1)
            for line in proc.stdout:
                line_str = line.strip()
                if "trycloudflare.com" in line_str:
                    match = re.search(r'https://[a-zA-Z0-9-]+\.trycloudflare\.com', line_str)
                    if match:
                        tunnel_url = match.group(0)
                        print("\n" + "="*60, flush=True)
                        print(f"CLOUDFLARE PUBLIC TUNNEL LIVE URL: {tunnel_url}", flush=True)
                        print("="*60 + "\n", flush=True)
                        os.makedirs("scratch", exist_ok=True)
                        with open("scratch/cloudflare_url.txt", "w") as f:
                            f.write(tunnel_url)
                else:
                    if "INF" in line_str or "ERR" in line_str:
                        print(line_str, flush=True)
            proc.wait()
        except Exception as e:
            print("Cloudflare tunnel process error:", e)
        print("Reconnecting Cloudflare tunnel in 3 seconds...")
        time.sleep(3)

if __name__ == '__main__':
    run_cloudflare_tunnel()
