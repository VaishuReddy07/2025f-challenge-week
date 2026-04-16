## FTRC-C003: STRIDE Analysis (Detailed)

###  Main Flow
User → Sends request (API key) → Flask API → Database → Response

---

###  Spoofing (Impersonation)

**Description:**  
Spoofing occurs when an attacker pretends to be a legitimate user.

**Where it happens:**  
- Authorization header: `Bearer <API_KEY>`

**Why it is vulnerable:**  
- The system uses a single static API key  
- No user-specific authentication  
- API key can be leaked or reused  

**Impact:**  
- Unauthorized access to all endpoints  
- Attacker can act as any user  
- Full control over system functionality  

---

###  Tampering (Data Manipulation)

**Description:**  
Tampering involves unauthorized modification of data.

**Where it happens:**  
- POST /workouts  
- PATCH /workouts/<id>  
- DELETE /workouts/<id>  

**Why it is vulnerable:**  
- User input directly affects database operations  
- Limited validation on incoming data  

**Impact:**  
- Data corruption  
- Unauthorized updates or deletions  
- Loss of data integrity  

---

###  Repudiation (No Accountability)

**Description:**  
Repudiation occurs when users can deny their actions due to lack of tracking.

**Where it happens:**  
- All endpoints (no logging implemented)

**Why it is vulnerable:**  
- No audit logs  
- No user activity tracking  

**Impact:**  
- Cannot trace malicious actions  
- No evidence for debugging or security incidents  

---

###  Information Disclosure (Data Leakage)

**Description:**  
Sensitive information may be exposed to unauthorized users.

**Where it happens:**  
- Debug mode (if enabled)  
- API responses  
- Error handling  

**Why it is vulnerable:**  
- Debug mode can reveal stack traces  
- Responses may expose internal data  

**Impact:**  
- Exposure of internal system details  
- Leakage of user data  
- Helps attackers plan further attacks  

---

###  Denial of Service (DoS)

**Description:**  
Attackers attempt to overwhelm the system with excessive requests.

**Where it happens:**  
- All API endpoints  

**Current protection:**  
- Basic rate limiting (IP-based)

**Remaining weakness:**  
- Can be bypassed using proxies or multiple IPs  

**Impact:**  
- Service slowdown  
- System unavailability  
- Poor user experience  

---

### 🟩 Elevation of Privilege (Unauthorized Access Levels)

**Description:**  
Users gain higher privileges than intended.

**Where it happens:**  
- No role-based access control  
- All endpoints accessible with same API key  

**Why it is vulnerable:**  
- No distinction between user roles (admin/user)  
- Single API key grants full access  

**Impact:**  
- Unauthorized admin actions  
- Full control over application data  
- Increased risk of system compromise  

---




