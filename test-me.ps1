try {
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/me" -Method Get
    Write-Host "Success: $res"
} catch {
    Write-Host "Status code: $($_.Exception.Response.StatusCode.value__)"
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Body: $($reader.ReadToEnd())"
}
