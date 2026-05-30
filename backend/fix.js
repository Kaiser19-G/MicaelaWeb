const headers = {
    'apikey': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV0dXJpenRtdXhwa2xtbGt5ZmlpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3OTgzNzM0NSwiZXhwIjoyMDk1NDEzMzQ1fQ.iay3Cf5O7sGXrIYHbiWecC55v78UWROb2mXAzQegV6g',
    'Authorization': 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV0dXJpenRtdXhwa2xtbGt5ZmlpIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3OTgzNzM0NSwiZXhwIjoyMDk1NDEzMzQ1fQ.iay3Cf5O7sGXrIYHbiWecC55v78UWROb2mXAzQegV6g',
    'Content-Type': 'application/json'
};

const hash = '$2a$12$fjAvA.xUbhX6ODbieetCD.cU8bloi5i5fs6jUrgIRn995QXQ3Tjku';

fetch('https://eturiztmuxpklmlkyfii.supabase.co/rest/v1/usuarios?username=in.(docente1,u22220224,alumno1)', {
    method: 'PATCH',
    headers: headers,
    body: JSON.stringify({ password: hash })
}).then(res => console.log('Updated:', res.status));
