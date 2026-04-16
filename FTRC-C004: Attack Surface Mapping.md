## FTRC-C004: Attack Surface Mapping 

### Description
The attack surface includes all entry points where untrusted data enters the system. These inputs can be controlled or manipulated by attackers.

---

### Untrusted Inputs

#### 1. Authorization Header (API Key)

Example:
Authorization: Bearer <API_KEY>

- Used in all API requests  
- Comes directly from the client  

Risk:
- API key theft  
- User impersonation  
- Unauthorized access  

---

#### 2. JSON Request Body

Example:
{
  "date": "2026-04-15",
  "duration_min": 45,
  "notes": "Leg day",
  "exercises": [...]
}

Used in:
- POST /workouts  
- PATCH /workouts/<id>  
- POST /workouts/import  

Risk:
- Malicious input  
- Data manipulation  
- Application crashes  

---

#### 3. Query Parameters

Example:
/workouts?from=2026-01-01&to=2026-04-15

Used in:
- GET /workouts  
- GET /workouts/search  

Risk:
- Input manipulation  
- Unexpected behavior  
- Filtering abuse  

---

#### 4. Path Parameters

Example:
/workouts/<id>

Used in:
- GET /workouts/<id>  
- PATCH /workouts/<id>  
- DELETE /workouts/<id>  

Risk:
- Accessing unauthorized data  
- ID manipulation  
- Unauthorized operations  

---

#### 5. Base64 Import Payload

Example:
{
  "data": "base64_encoded_string"
}

Used in:
- POST /workouts/import  

Risk:
- Large payload attacks (DoS)  
- Malformed data  
- Parsing failures  

---

#### 6. Client IP Address

Used in:
- Rate limiting  

Risk:
- IP spoofing  
- Rate limit bypass  
- Distributed attacks  

---

#### 7. Admin Page Input

Example:
User notes displayed in HTML

Used in:
- /admin page  

Risk:
- Cross-Site Scripting (XSS)  
- Client-side attacks  

---

### Key Risks

- Data manipulation  
- Unauthorized access  
- Injection attacks  
- Denial of Service (DoS)  

---


| Input Type | Source | Risk Level |
|-----------|-------|-----------|
| Authorization Header | Client | High |
| JSON Body | Client | High |
| Query Parameters | URL | Medium |
| Path Parameters | URL | High |
| Base64 Payload | Client | High |
| Client IP | Network | Medium |
| Admin Input | Stored/User | Medium |

---

