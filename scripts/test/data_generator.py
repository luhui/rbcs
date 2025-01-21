import argparse
import random
import mysql.connector
from datetime import datetime
from mysql.connector import Error

def generate_accounts(num_records):
    accounts = []

    for i in range(num_records):
        account_number = random.randint(100000000, 999999999)
        balance = random.randint(100, 1000000)
        create_at = updated_at = datetime.now()
        status = 'ACTIVATED'
        version = 1
        accounts.append((account_number, balance, create_at, updated_at, status, version))

    return accounts

def insert_data(db_config, accounts):
    try:
        connection = mysql.connector.connect(**db_config)
        cursor = connection.cursor()

        # Insert accounts
        account_query = """
                    INSERT INTO account (account_number, balance, created_at, updated_at, status, version)
                    VALUES (%s, %s, %s, %s, %s, %s)
                """
        cursor.executemany(account_query, accounts)

        # Fetch the inserted AccountIds
        cursor.execute(f"SELECT id FROM account ORDER BY created_at DESC LIMIT {len(accounts)}")
        account_ids = [row[0] for row in cursor.fetchall()]

        # Generate transactions using the fetched AccountIds
        transactions = [(account_id, random.randint(100, account_id * 10), 'completed', 'deposit', datetime.now(), datetime.now(), 1) for account_id in account_ids]

        # Insert transactions
        transaction_query = """
            INSERT INTO transaction
            (source_account_id, amount, status, type, created_at, updated_at, version)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """
        cursor.executemany(transaction_query, transactions)

        connection.commit()
        print(f"Successfully inserted {len(accounts)} accounts and transactions")

    except Error as e:
        print(f"Error: {e}")
    finally:
        if connection.is_connected():
            cursor.close()
            connection.close()

def main():
    parser = argparse.ArgumentParser(description='Generate random account and transaction data')
    parser.add_argument('--host', default='localhost', help='MySQL host')
    parser.add_argument('--port', type=int, default=3306, help='MySQL port')
    parser.add_argument('--database', default='rbcs', help='Database name')
    parser.add_argument('--user', default='root', help='Database user')
    parser.add_argument('--password', default='root', help='Database password')
    parser.add_argument('--count', type=int, default='10000', help='Number of records to generate')

    args = parser.parse_args()

    db_config = {
        'host': args.host,
        'port': args.port,
        'database': args.database,
        'user': args.user,
        'password': args.password
    }

    accounts = generate_accounts(args.count)
    insert_data(db_config, accounts)

if __name__ == "__main__":
    main()