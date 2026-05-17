Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile('d:\jekjek\assets\MC\idle.png')
Write-Output "idle: $($img.Width)x$($img.Height)"
$img2 = [System.Drawing.Image]::FromFile('d:\jekjek\assets\MC\run.png')
Write-Output "run: $($img2.Width)x$($img2.Height)"
