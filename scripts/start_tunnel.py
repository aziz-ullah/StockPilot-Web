import subprocess
import time
import os


def run_persistent_tunnel():
    node_path = r"C:\Users\User\nodejs\node-v20.18.0-win-x64"
    env = os.environ.copy()
    env["PATH"] = f"{node_path};{env.get('PATH', '')}"

    cmd = [
        os.path.join(node_path, "npx.cmd"),
        "--yes",
        "localtunnel",
        "--port",
        "3000",
        "--subdomain",
        "stockpilot-apparel-client-demo",
    ]

    print("Starting persistent localtunnel daemon...")

    while True:
        try:
            print("Connecting localtunnel...")
            proc = subprocess.Popen(
                cmd,
                env=env,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
            )
            for line in proc.stdout:
                print(line.strip(), flush=True)
            proc.wait()
        except Exception as e:
            print("Tunnel connection error:", e)
        print("Reconnecting in 3 seconds...")
        time.sleep(3)


if __name__ == "__main__":
    run_persistent_tunnel()
