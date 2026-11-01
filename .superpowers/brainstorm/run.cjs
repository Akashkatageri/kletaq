const path = require('path');
const sessionDir = path.resolve(__dirname, 'session-1');
process.env.BRAINSTORM_DIR = sessionDir;
process.env.BRAINSTORM_OPEN = '1';
process.env.BRAINSTORM_HOST = '127.0.0.1';
process.env.BRAINSTORM_URL_HOST = 'localhost';

require('../../.agents/plugins/superpowers/skills/brainstorming/scripts/server.cjs');
