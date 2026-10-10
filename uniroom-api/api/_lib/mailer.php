<?php
/*
|--------------------------------------------------------------------------
| Small SMTP mailer (works with Gmail)
|--------------------------------------------------------------------------
| No Composer packages needed. Settings come from config.php:
|   SMTP_HOST=smtp.gmail.com  SMTP_PORT=465  SMTP_SECURE=ssl
|   SMTP_USER=yourname@gmail.com  SMTP_PASS=<16-letter Google App Password>
|
| One Mailer object keeps the connection open, so bulk account creation
| can send many emails quickly.
*/

class Mailer
{
    private $socket = null;
    private $cfg;
    public $lastError = '';

    public function __construct(array $smtpConfig)
    {
        $this->cfg = $smtpConfig;
    }

    public function isConfigured(): bool
    {
        return $this->cfg['username'] !== '' && $this->cfg['password'] !== '';
    }

    /** Sends one email. Returns true on success; otherwise see $lastError. */
    public function send(string $toEmail, string $toName, string $subject, string $html, string $text): bool
    {
        try {
            if (!$this->isConfigured()) {
                throw new RuntimeException('Email is not set up yet (SMTP_USER / SMTP_PASS are empty).');
            }
            if ($this->socket === null) $this->connect();

            $from = $this->cfg['from'];
            $this->cmd('MAIL FROM:<' . $from . '>', [250]);
            $this->cmd('RCPT TO:<' . $toEmail . '>', [250, 251]);
            $this->cmd('DATA', [354]);

            $boundary = 'uniroom_' . bin2hex(random_bytes(8));
            $headers = [
                'Date: ' . date('r'),
                'From: ' . $this->encodeName($this->cfg['from_name']) . ' <' . $from . '>',
                'To: ' . $this->encodeName($toName) . ' <' . $toEmail . '>',
                'Subject: ' . $this->encodeName($subject),
                'Message-ID: <' . bin2hex(random_bytes(12)) . '@' . (explode('@', $from)[1] ?? 'uniroom') . '>',
                'MIME-Version: 1.0',
                'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
            ];
            $body = "--$boundary\r\n"
                . "Content-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"
                . chunk_split(base64_encode($text))
                . "--$boundary\r\n"
                . "Content-Type: text/html; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"
                . chunk_split(base64_encode($html))
                . "--$boundary--\r\n";

            $data = implode("\r\n", $headers) . "\r\n\r\n" . $body;
            // A line that starts with "." must be doubled (SMTP rule).
            $data = preg_replace('/^\./m', '..', $data);
            fwrite($this->socket, $data . "\r\n.\r\n");
            $this->expect([250]);
            return true;
        } catch (Throwable $e) {
            $this->lastError = $e->getMessage();
            $this->close();
            return false;
        }
    }

    public function close(): void
    {
        if ($this->socket) {
            @fwrite($this->socket, "QUIT\r\n");
            @fclose($this->socket);
        }
        $this->socket = null;
    }

    public function __destruct()
    {
        $this->close();
    }

    // ------------------------------------------------------------------

    private function connect(): void
    {
        $host = $this->cfg['host'];
        $port = $this->cfg['port'];
        $secure = strtolower($this->cfg['secure']);
        $target = ($secure === 'ssl' ? 'ssl://' : 'tcp://') . $host . ':' . $port;

        $ctx = stream_context_create(['ssl' => ['verify_peer' => true, 'verify_peer_name' => true]]);
        $this->socket = @stream_socket_client($target, $errno, $errstr, 15, STREAM_CLIENT_CONNECT, $ctx);
        if (!$this->socket) {
            throw new RuntimeException("Could not connect to the mail server ($errstr).");
        }
        stream_set_timeout($this->socket, 20);
        $this->expect([220]);
        $this->cmd('EHLO uniroom', [250]);

        if ($secure === 'tls') {
            $this->cmd('STARTTLS', [220]);
            if (!stream_socket_enable_crypto($this->socket, true, STREAM_CRYPTO_METHOD_TLS_CLIENT)) {
                throw new RuntimeException('Could not start a secure connection to the mail server.');
            }
            $this->cmd('EHLO uniroom', [250]);
        }

        $this->cmd('AUTH LOGIN', [334]);
        $this->cmd(base64_encode($this->cfg['username']), [334]);
        $this->cmd(base64_encode($this->cfg['password']), [235], 'The mail server rejected the username or App Password.');
    }

    private function cmd(string $line, array $okCodes, string $failMessage = ''): string
    {
        fwrite($this->socket, $line . "\r\n");
        return $this->expect($okCodes, $failMessage);
    }

    private function expect(array $okCodes, string $failMessage = ''): string
    {
        $response = '';
        while (($line = fgets($this->socket, 515)) !== false) {
            $response .= $line;
            // Multi-line replies look like "250-..." and end with "250 ..."
            if (strlen($line) < 4 || $line[3] === ' ') break;
        }
        $code = (int) substr($response, 0, 3);
        if (!in_array($code, $okCodes, true)) {
            throw new RuntimeException($failMessage !== '' ? $failMessage : 'Mail server said: ' . trim($response));
        }
        return $response;
    }

    private function encodeName(string $s): string
    {
        $s = str_replace(["\r", "\n"], ' ', $s);
        if (preg_match('/[^\x20-\x7E]/', $s)) return '=?UTF-8?B?' . base64_encode($s) . '?=';
        if (preg_match('/[()<>@,;:\\\\".\[\]]/', $s)) return '"' . addcslashes($s, '"\\') . '"';
        return $s;
    }
}
