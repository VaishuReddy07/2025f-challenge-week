# Security Vulnerabilities in app.py

## 🔴 Critical Vulnerabilities

### 1. Insecure Deserialization (Lines 329-333)
**Severity:** CRITICAL

**Location:** `/workouts/import` endpoint

**Vulnerable Code:**
```python
@app.route("/workouts/import", methods=["POST"])
def import_workouts():
    data = request.get_json()
    payload = data.get("data", "")
    
    workout_data = pickle.loads(base64.b64decode(payload))
    
    return jsonify({"message": f"Imported {len(workout_data)} workouts"})
```

**Issue:** Using `pickle.loads()` on untrusted user input allows **arbitrary code execution**. An attacker can craft a malicious pickle payload that executes arbitrary Python code when deserialized.

**Recommended Fix:**
```python
import json

@app.route("/workouts/import", methods=["POST"])
def import_workouts():
    data = request.get_json()
    payload = data.get("data", "")
    
    # Use JSON instead of pickle for untrusted data
    try:
        workout_data = json.loads(base64.b64decode(payload).decode('utf-8'))
        return jsonify({"message": f"Imported {len(workout_data)} workouts"})
    except (json.JSONDecodeError, ValueError) as e:
        return jsonify({"error": "Invalid payload format"}), 400
```

---

### 2. SQL Injection via Unparameterized Column Names (Lines 306-315)
**Severity:** CRITICAL

**Location:** `/workouts/<int:workout_id>` PATCH endpoint

**Vulnerable Code:**
```python
@app.route("/workouts/<int:workout_id>", methods=["PATCH"])
def update_workout(workout_id):
    data = request.get_json()
    
    conn = get_db()
    cursor = conn.cursor()
    
    fields = []
    values = []
    
    for key, value in data.items():
        fields.append(f"{key} = %s")  # ⚠️ Column names not validated!
        values.append(value)
    
    values.append(workout_id)
    
    cursor.execute(
        f"UPDATE workouts SET {', '.join(fields)} WHERE id = %s",
        values
    )
```

**Issue:** User-controlled column names can inject SQL code. An attacker can pass malicious keys that modify the SQL query structure or execute arbitrary SQL.

**Attack Example:**
```json
{
  "date) OR 1=1 -- ": "value"
}
```

**Recommended Fix:**
```python
@app.route("/workouts/<int:workout_id>", methods=["PATCH"])
def update_workout(workout_id):
    data = request.get_json()
    
    # Whitelist allowed columns
    ALLOWED_FIELDS = {"date", "duration_min", "notes"}
    
    conn = get_db()
    cursor = conn.cursor()
    
    fields = []
    values = []
    
    for key, value in data.items():
        if key not in ALLOWED_FIELDS:
            return jsonify({"error": f"Invalid field: {key}"}), 400
        fields.append(f"{key} = %s")
        values.append(value)
    
    if not fields:
        return jsonify({"error": "No valid fields to update"}), 400
    
    values.append(workout_id)
    
    cursor.execute(
        f"UPDATE workouts SET {', '.join(fields)} WHERE id = %s",
        values
    )
    
    conn.commit()
    cursor.close()
    conn.close()
    
    return jsonify({"message": "Workout updated"})
```

---

### 3. Hardcoded Secret Keys (Lines 10-11)
**Severity:** CRITICAL

**Location:** Application initialization

**Vulnerable Code:**
```python
app.secret_key = "changeme"
SECRET_KEY = "super-secret-key-123"
```

**Issue:** 
- Hardcoded credentials in source code expose secrets to anyone with access to the repository
- Weak and predictable secret keys compromise session security and JWT tokens
- Credentials cannot be rotated without code changes

**Recommended Fix:**
```python
import os
from dotenv import load_dotenv

load_dotenv()

app.secret_key = os.getenv("FLASK_SECRET_KEY")
if not app.secret_key:
    raise ValueError("FLASK_SECRET_KEY environment variable not set")

if not app.secret_key or app.secret_key == "changeme":
    raise ValueError("Flask secret key must be set to a strong random value")
```

**Create `.env` file (add to `.gitignore`):**
```
FLASK_SECRET_KEY=your-secure-random-key-here-min-32-characters
```

---

## 🟡 High Severity Vulnerabilities

### 4. Cross-Site Scripting (XSS) (Lines 271-277)
**Severity:** HIGH

**Location:** `/admin` endpoint

**Vulnerable Code:**
```python
@app.route("/admin")
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("SELECT * FROM workouts ORDER BY date DESC")
    workouts = cursor.fetchall()
    
    cursor.close()
    conn.close()
    
    html = "<html><body><h1>Admin Panel</h1>"
    for w in workouts:
        html += f"<div><h3>{w['date']}</h3><p>{w['notes']}</p></div>"  # ⚠️ Not escaped!
    html += "</body></html>"
    
    return html
```

**Issue:** User-controlled data in the `notes` field is directly embedded in HTML without escaping, allowing attackers to inject malicious JavaScript.

**Attack Example:**
```
Workout notes: <img src=x onerror="alert('XSS Attack')">
```

**Recommended Fix:**
```python
from flask import render_template_string
from markupsafe import escape

@app.route("/admin")
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("SELECT * FROM workouts ORDER BY date DESC")
    workouts = cursor.fetchall()
    
    cursor.close()
    conn.close()
    
    # Escape HTML entities in user data
    escaped_workouts = [
        {
            **w,
            'notes': escape(w['notes'] or ''),
            'date': escape(str(w['date']))
        }
        for w in workouts
    ]
    
    html = "<html><body><h1>Admin Panel</h1>"
    for w in escaped_workouts:
        html += f"<div><h3>{w['date']}</h3><p>{w['notes']}</p></div>"
    html += "</body></html>"
    
    return html
```

**Better Approach - Use Templates:**
```python
@app.route("/admin")
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    
    cursor.execute("SELECT * FROM workouts ORDER BY date DESC")
    workouts = cursor.fetchall()
    
    cursor.close()
    conn.close()
    
    return render_template('admin.html', workouts=workouts)
```

Create `templates/admin.html`:
```html
<!DOCTYPE html>
<html>
<body>
    <h1>Admin Panel</h1>
    {% for workout in workouts %}
        <div>
            <h3>{{ workout.date }}</h3>
            <p>{{ workout.notes }}</p>
        </div>
    {% endfor %}
</body>
</html>
```

---

### 5. Missing Authentication & Authorization
**Severity:** HIGH

**Location:** All endpoints

**Issue:** No authentication mechanism exists. Any client can:
- View all workouts (`GET /workouts`)
- Create new workouts (`POST /workouts`)
- Delete any workout (`DELETE /workouts/<id>`)
- Update workouts (`PATCH /workouts/<id>`)
- Access admin panel (`GET /admin`)

**Recommended Fix - JWT Authentication:**
```python
from flask_jwt_extended import JWTManager, create_access_token, jwt_required
from werkzeug.security import generate_password_hash, check_password_hash

app.config['JWT_SECRET_KEY'] = os.getenv('JWT_SECRET_KEY', 'change-me')
jwt = JWTManager(app)

@app.route("/login", methods=["POST"])
def login():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    
    # Verify credentials against database
    if verify_user(username, password):
        access_token = create_access_token(identity=username)
        return jsonify({"access_token": access_token}), 200
    return jsonify({"error": "Invalid credentials"}), 401

@app.route("/workouts", methods=["GET"])
@jwt_required()
def list_workouts():
    # ... existing code
    return jsonify(workouts)
```

---

## 🟠 Medium Severity Vulnerabilities

### 6. Server Fingerprinting (Lines 16-18)
**Severity:** MEDIUM

**Location:** `add_header()` function

**Vulnerable Code:**
```python
@app.after_request
def add_header(response):
    response.headers["X-Powered-By"] = "Flask/2.3.2 Python/3.11"
    return response
```

**Issue:** Revealing technology stack (Flask version, Python version) helps attackers identify known vulnerabilities specific to those versions.

**Recommended Fix:**
```python
@app.after_request
def add_header(response):
    # Remove or set to generic value
    response.headers["X-Powered-By"] = "Application Server"
    # Optionally remove it entirely
    response.headers.pop("Server", None)
    return response
```

---

### 7. Debug Mode Enabled in Production (Line 348)
**Severity:** MEDIUM

**Location:** Application startup

**Vulnerable Code:**
```python
if __name__ == "__main__":
    app.run(debug=True, port=5000)
```

**Issue:** Debug mode:
- Exposes stack traces with sensitive information
- Allows interactive debugger access
- Enables code reloading on file changes
- Reveals internal application structure

**Recommended Fix:**
```python
if __name__ == "__main__":
    debug_mode = os.getenv('FLASK_DEBUG', 'False').lower() == 'true'
    app.run(debug=debug_mode, port=5000, host='0.0.0.0')
```

**Set in `.env`:**
```
FLASK_DEBUG=False  # Only True during development
```

---

## Summary Table

| # | Vulnerability | Severity | Type | Fix Priority |
|---|---|---|---|---|
| 1 | Insecure Deserialization | CRITICAL | Code Injection | P0 |
| 2 | SQL Injection | CRITICAL | Injection | P0 |
| 3 | Hardcoded Secrets | CRITICAL | Credential Exposure | P0 |
| 4 | Cross-Site Scripting (XSS) | HIGH | Injection | P1 |
| 5 | Missing Authentication | HIGH | Access Control | P1 |
| 6 | Server Fingerprinting | MEDIUM | Information Disclosure | P2 |
| 7 | Debug Mode Enabled | MEDIUM | Information Disclosure | P2 |

---

## Recommended Actions

### Immediate (P0 - CRITICAL)
- [ ] Remove pickle deserialization usage
- [ ] Implement column whitelist for UPDATE queries
- [ ] Move secrets to environment variables

### Short-term (P1 - HIGH)
- [ ] Implement HTML escaping for user data
- [ ] Add JWT authentication to all endpoints
- [ ] Implement role-based access control (RBAC)

### Medium-term (P2 - MEDIUM)
- [ ] Remove or obscure version headers
- [ ] Disable debug mode in production
- [ ] Implement comprehensive input validation
- [ ] Add security headers (CSP, HSTS, X-Frame-Options)
- [ ] Add rate limiting
- [ ] Implement logging and monitoring
