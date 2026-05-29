const bcrypt = require('bcryptjs');
const hash = '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lVWy';
const pass = 'admin123';
console.log(bcrypt.compareSync(pass, hash) ? 'MATCH' : 'NO MATCH');
