$env:JAVA_HOME = 'E:\JAVA project\tools\jdk17'
$env:MAVEN_HOME = 'E:\JAVA project\tools\maven'
$javaBin = Join-Path $env:JAVA_HOME 'bin'
$mavenBin = Join-Path $env:MAVEN_HOME 'bin'
$env:Path = "$javaBin;$mavenBin;$env:Path"
Set-Location 'E:\JAVA project\blog-backend'
& (Join-Path $mavenBin 'mvn.cmd') -gs 'E:\JAVA project\tools\maven-conf\settings.xml' -B spring-boot:run
