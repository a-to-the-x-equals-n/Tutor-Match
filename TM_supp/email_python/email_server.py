import socket
import os
from welcome_email import send_email



HOST = "localhost"
PORT = 12345


def main():


    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((HOST, PORT))
    server_socket.listen(1)

    os.system("cls")

    print("Server listening on", (HOST, PORT))

    client_socket, address = server_socket.accept()

    print("Connection from", address)


    # Receive multiple string values
    name, email, subject = client_socket.recv(1024).decode('utf-8')[2:].split(" ")


    send_email(name, email, subject)

    client_socket.close()
    server_socket.close()

if __name__ == "__main__":
    main()
