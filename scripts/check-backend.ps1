$urls = @(
  'http://localhost:8090/actuator/health',
  'http://localhost:5173/api/admin/me',
  'http://localhost:5173/api/tags'
)
foreach ($url in $urls) {
  try {
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
    Write-Host ("OK   {0} -> {1}" -f $url, $response.StatusCode)
  } catch {
    Write-Host ("FAIL {0} -> {1}" -f $url, $_.Exception.Message)
  }
}
