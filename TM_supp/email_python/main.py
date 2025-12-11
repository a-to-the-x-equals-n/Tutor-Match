from datetime import date, timedelta
import pandas as pd
from email_python.welcome_email import send_email


# email: tutor.match4230
# pass: tut0rmatch4230


# ===========================================================================
''' 

    NOT TESTED : alpha phase

'''
# ===========================================================================

data = ""


def load_df(data):

    parse_dates = ["session_date", "present_date"]
    df = pd.read_csv(data, parse_dates = parse_dates)

    return df


print(load_df(data))


def query_data_and_send_emails(df):

    present = date.today()
    reminder_date = present - timedelta(days = 1)

    for _, row in df.iterrows():

        if (row['session_date'].date() <= reminder_date):

            send_email(
                subject = f'Session Reminder for {row['name']}',
                receiver_email = row['email'],
                name = row['name'],
                session_date = row['session_date'],
            )


df = load_df(data)
result = query_data_and_send_emails(df)
print(result)