from datetime import date
import inspect
import os


# Constructs the absolute path to the log.txt file
log_file_path = os.path.join(os.path.dirname(__file__), "log.txt")


# Opens 'log.txt' and appends the error
def log(line, msg):
    # gets and formats current date as a string
    present_date = date.today().strftime('%Y / %m / %d') 

    # Write the log message to the file
    with open(log_file_path, "a") as file:
        file.write(f'{present_date} \nERROR: {msg} \nLINE: {line}')



# Custom exception class for errors related to .env file
class EnvFileError(Exception):

    # Constructor method for initializing 'EnvFileError'
    def __init__(self, message):

        # error message
        self.message = message 

        # line number where error occurred
        self.line_number = inspect.currentframe().f_back.f_lineno 

        # Call 'log' function to record the error
        log(self.line_number, self.message)




# Custom exception class for errors related to email sending
class EmailSendingError(Exception):

    # Constructor method for initializing 'EmailSendingError'
    def __init__(self, message):

        # error message
        self.message = message 

        # line number where error occurred
        self.line_number = inspect.currentframe().f_back.f_lineno 

        # Call 'log' function to record the error
        log(self.line_number, self.message)