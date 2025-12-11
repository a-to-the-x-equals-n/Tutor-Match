import os
import smtplib
from email.message import EmailMessage
from email.utils import formataddr
from pathlib import Path
from dotenv import load_dotenv
from custom_errors import EnvFileError, EmailSendingError



# Load the Environment variables
try:

    # Determine the current directory of the script and locate the .env file
    current_dir = Path(__file__).resolve().parent if "__file__" in locals() else Path.cwd()
    envars = current_dir / ".env"

    # Load the environment variables from the .env file
    load_dotenv(envars)


# If any exception occurs during the process, raise an EnvFileError
except Exception as e:
    
    raise EnvFileError(f'Error opening .env file: {str(e)}')


# Read environment variables
sender_email = os.getenv("EMAIL")
password_email = os.getenv("PASSWORD")


# Send Email
def send_email(name, receiver_email, subject):

    # Gmail server and port number
    port = 587
    email_server = "smtp.gmail.com"

    # Set email Fields
    msg = EmailMessage()
    msg['Subject'] = subject
    msg['From'] = formataddr(('Tutor Match', f"{sender_email}"))
    msg['To'] = receiver_email
    msg['BCC'] = sender_email

    # Create the base text message
    msg.set_content(
        f"""\
        Hi {name},
        Welcome to Tutor Match!
        Let's get these grades fam.
        - Tutor Match
        """
    )

    # HTML version  
    msg.add_alternative(
        f"""\
    <html>
        <body>
            <p> Hi {name}, </p>
            <p> Welcome to Tutor Match!</p>
            <p>Let's get these grades fam.</p>
            <p>Tutor Match</p>
        </body>
    </html>
    """,
        subtype = "html" 
    )


    # Attempt to establish a connection with the SMTP server and send an email
    try:
        with smtplib.SMTP(email_server, port) as server:

            # Start TLS encryption for security
            server.starttls()

            # Log in to the SMTP server using the sender's email and password
            server.login(sender_email, password_email)

            # send email
            server.sendmail(sender_email, receiver_email, msg.as_string())


    # If any exception occurs during the email sending process raise custom 'EmailSendingError'
    except Exception as e:

        raise EmailSendingError(f'Error sending email: {str(e)}')

    # Regardless of exception or not, close the SMTP connection
    finally:
            server.close()


    
if __name__ == "__main__":

    send_email(
        name = "Logan",
        receiver_email = "reinel22@students.ecu.edu",
        subject = "Test Mail" 
    )


'''
'__name__' is a special built-in variable in Python that represents the name of the current module

When a Python script is ran directly, '__name__' is set to '__main__'

The expression __name__ == "__main__" checks if the current script is the main program being executed

If the script is being run directly, the code inside the 'if' block will execute

'''