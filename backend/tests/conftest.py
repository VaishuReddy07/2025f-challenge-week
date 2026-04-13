import os
import sys
import pytest
import mysql.connector

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from app import app as flask_app
from seed import seed

TEST_DB_NAME = "test_fitness_tracker"


@pytest.fixture()
def app():
    """Create an application instance with a fresh test database."""
    import database

    # Create test database
    config_no_db = {k: v for k, v in database.DB_CONFIG.items() if k != "database"}
    conn = mysql.connector.connect(**config_no_db)
    cursor = conn.cursor()
    cursor.execute(f"DROP DATABASE IF EXISTS {TEST_DB_NAME}")
    cursor.execute(f"CREATE DATABASE {TEST_DB_NAME}")
    cursor.close()
    conn.close()

    # Point app at test database
    database.DB_CONFIG["database"] = TEST_DB_NAME
    seed(TEST_DB_NAME)

    flask_app.config["TESTING"] = True
    yield flask_app

    # Cleanup
    conn = mysql.connector.connect(**config_no_db)
    cursor = conn.cursor()
    cursor.execute(f"DROP DATABASE IF EXISTS {TEST_DB_NAME}")
    cursor.close()
    conn.close()


@pytest.fixture()
def client(app):
    """A Flask test client."""
    return app.test_client()
