import socket

target = "127.0.0.1"
ports = [80, 443, 5000]

for port in ports:
    s = socket.socket()
    try:
        s.connect((target, port))
        print(f"[+] Port {port} is OPEN")
    except:
        print(f"[-] Port {port} is CLOSED")