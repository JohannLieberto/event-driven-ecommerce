# =============================================================================
# start-jenkins-tunnel.ps1
# Starts ngrok tunnel to expose Jenkins on port 8083,
# then verifies the GitHub webhook is reachable.
#
# Usage:
#   .\jenkins\start-jenkins-tunnel.ps1
#
# Requirements:
#   - ngrok installed and authenticated (ngrok config add-authtoken <token>)
#   - Jenkins running on localhost:8083
# =============================================================================

$NGROK_DOMAIN  = "horrific-kimbery-composedly.ngrok-free.dev"
$JENKINS_PORT  = 8083
$WEBHOOK_PATH  = "/github-webhook/"
$WEBHOOK_URL   = "https://$NGROK_DOMAIN$WEBHOOK_PATH"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Jenkins ngrok Tunnel Launcher" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- Check ngrok is installed ---
if (-not (Get-Command ngrok -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] ngrok not found. Install it from the Microsoft Store or https://ngrok.com/download" -ForegroundColor Red
    exit 1
}

# --- Check Jenkins is actually running on the expected port ---
Write-Host "[1/4] Checking Jenkins is running on port $JENKINS_PORT..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:$JENKINS_PORT" -TimeoutSec 5 -ErrorAction Stop
    Write-Host "      Jenkins is UP (HTTP $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Jenkins did not respond on port $JENKINS_PORT. Make sure Docker Desktop is running." -ForegroundColor Yellow
    Write-Host "       Continuing anyway — ngrok will still start." -ForegroundColor Yellow
}

# --- Kill any existing ngrok processes ---
Write-Host ""
Write-Host "[2/4] Stopping any existing ngrok processes..." -ForegroundColor Yellow
Get-Process -Name ngrok -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 1
Write-Host "      Done." -ForegroundColor Green

# --- Start ngrok in the background ---
Write-Host ""
Write-Host "[3/4] Starting ngrok tunnel..." -ForegroundColor Yellow
Write-Host "      Domain : $NGROK_DOMAIN" -ForegroundColor White
Write-Host "      Port   : $JENKINS_PORT" -ForegroundColor White

Start-Process -FilePath "ngrok" `
    -ArgumentList "http --domain=$NGROK_DOMAIN $JENKINS_PORT" `
    -WindowStyle Minimized

# Wait for ngrok to establish the tunnel
Write-Host "      Waiting for tunnel to establish..."
$maxWait = 15
$waited  = 0
$tunnelUp = $false
while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 1
    $waited++
    try {
        $ngrokApi = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -TimeoutSec 2 -ErrorAction Stop
        if ($ngrokApi.tunnels.Count -gt 0) {
            $tunnelUp = $true
            break
        }
    } catch { }
    Write-Host "      ...still waiting ($waited/$maxWait)s"
}

if (-not $tunnelUp) {
    Write-Host "[ERROR] ngrok tunnel did not start within $maxWait seconds." -ForegroundColor Red
    Write-Host "        Check ngrok is authenticated: ngrok config add-authtoken <your-token>" -ForegroundColor Red
    exit 1
}

Write-Host "      Tunnel is UP!" -ForegroundColor Green

# --- Test the webhook endpoint ---
Write-Host ""
Write-Host "[4/4] Testing GitHub webhook endpoint..." -ForegroundColor Yellow
Write-Host "      URL: $WEBHOOK_URL" -ForegroundColor White

try {
    # GitHub sends a ping with X-GitHub-Event: ping header
    $headers = @{
        "Content-Type"    = "application/json"
        "X-GitHub-Event"  = "ping"
        "X-Hub-Signature" = "sha1=test"
        "User-Agent"      = "GitHub-Hookshot/test"
    }
    $body = '{"zen":"Practicality beats purity.","hook_id":1,"hook":{"type":"Repository","events":["push"]}}'

    $result = Invoke-WebRequest -Uri $WEBHOOK_URL `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -TimeoutSec 10 `
        -ErrorAction Stop

    Write-Host ""
    Write-Host "  WEBHOOK OK (HTTP $($result.StatusCode))" -ForegroundColor Green
    Write-Host "  Jenkins received the ping successfully!" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value__
    if ($statusCode -eq 403 -or $statusCode -eq 302) {
        # Jenkins returns 403 on unauthenticated webhook pings — that means it IS reachable
        Write-Host ""
        Write-Host "  WEBHOOK REACHABLE (HTTP $statusCode)" -ForegroundColor Green
        Write-Host "  Jenkins is reachable! (403/302 is normal for unauthenticated pings)" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "  [WARN] Webhook returned: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "  Jenkins may not be fully started yet. Try again in a few seconds." -ForegroundColor Yellow
    }
}

# --- Summary ---
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TUNNEL RUNNING" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Public URL : https://$NGROK_DOMAIN" -ForegroundColor White
Write-Host "  Webhook    : $WEBHOOK_URL" -ForegroundColor White
Write-Host "  ngrok UI   : http://localhost:4040" -ForegroundColor White
Write-Host ""
Write-Host "  Set this in GitHub:" -ForegroundColor Yellow
Write-Host "  Settings -> Webhooks -> Payload URL:" -ForegroundColor Yellow
Write-Host "  $WEBHOOK_URL" -ForegroundColor Green
Write-Host ""
Write-Host "  Press Ctrl+C to stop (ngrok keeps running minimised)" -ForegroundColor DarkGray
Write-Host ""
