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
