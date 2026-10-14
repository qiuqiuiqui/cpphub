$connection = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue
if ($null -ne $connection) {
  $processId = $connection.OwningProcess
  Write-Host "Stopping process $processId holding port 8090"
  Stop-Process -Id $processId -Force
  Start-Sleep -Seconds 2
}
$remaining = Get-NetTCPConnection -LocalPort 8090 -State Listen -ErrorAction SilentlyContinue
if ($null -eq $remaining) {
  Write-Host "Port 8090 is now free."
} else {
  Write-Host "Port 8090 is still in use."
}
