import psycopg2

def init_postgres():
    try:
        conn = psycopg2.connect(dbname='postgres', user='postgres', host='127.0.0.1', port=5432)
        conn.autocommit = True
        cur = conn.cursor()
        
        cur.execute("ALTER USER postgres WITH PASSWORD 'postgres';")
        print("Set postgres user password to 'postgres'.")
        
        cur.execute("SELECT 1 FROM pg_database WHERE datname='stockpilot_db';")
        if not cur.fetchone():
            cur.execute("CREATE DATABASE stockpilot_db;")
            print("Created database 'stockpilot_db'.")
        else:
            print("Database 'stockpilot_db' already exists.")
            
        cur.close()
        conn.close()
        
        test_conn = psycopg2.connect(dbname='stockpilot_db', user='postgres', password='postgres', host='127.0.0.1', port=5432)
        print("Connected to 'stockpilot_db' successfully!")
        test_conn.close()
    except Exception as e:
        print("Error initializing database:", e)
        raise e

if __name__ == '__main__':
    init_postgres()
