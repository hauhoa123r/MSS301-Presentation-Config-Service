param(
    [string]$BaseUrl = 'http://localhost:8888',
    [string]$Label = 'main'
)
$ErrorActionPreference = 'Stop'
function Assert-Equal($Actual, $Expected, [string]$Message) {
    if ($Actual -cne $Expected) {
        throw "$Message : expected [$Expected], got [$Actual]"
    }
}
$base = $BaseUrl.TrimEnd('/')
$health = Invoke-RestMethod "$base/actuator/health" -TimeoutSec 30
Assert-Equal $health.status 'UP' 'Config Server health'
$rows = foreach ($service in @('job-service', 'candidate-service', 'application-service')) {
    foreach ($profile in @('dev', 'test', 'prod')) {
        $url = "$base/$service/$profile/$Label"
        $response = Invoke-RestMethod $url -TimeoutSec 45
        Assert-Equal $response.name $service 'Application name'
        if ($profile -notin $response.profiles) { throw "Wrong profile: $url" }
        if (-not $response.version) { throw "Missing Git commit version: $url" }
        $merged = @{}
        # Config Server lists higher-priority property sources first.
        foreach ($source in $response.propertySources) {
            foreach ($property in $source.source.PSObject.Properties) {
                if (-not $merged.ContainsKey($property.Name)) {
                    $merged[$property.Name] = $property.Value
                }
            }
        }
        Assert-Equal $merged['ats.environment'] $profile 'Environment'
        Assert-Equal $merged['ats.demo.message'] "$service - $($profile.ToUpper())" 'Demo value'
        Assert-Equal $merged['ats.config.source'] 'ats-config-repo' 'Common config'
        Assert-Equal $merged['spring.datasource.driver-class-name'] 'org.postgresql.Driver' 'Service base config'
        $prefix = switch ($service) {
            'job-service' { 'JOB' }
            'candidate-service' { 'CANDIDATE' }
            'application-service' { 'APPLICATION' }
        }
        $expectedPassword = '${' + $prefix + '_DB_PASSWORD}'
        Assert-Equal $merged['spring.datasource.password'] $expectedPassword 'Secret placeholder'
        if ($profile -eq 'test' -and $merged['spring.datasource.url'] -notlike '*_sdb_test*') {
            throw "Test database is not isolated: $url"
        }
        if ($profile -eq 'prod') {
            Assert-Equal $merged['spring.datasource.url'] ('${' + $prefix + '_DB_URL}') 'Production database'
        }
        if ($service -eq 'application-service') {
            if (-not $merged.ContainsKey('job-service.url') -or -not $merged.ContainsKey('candidate-service.url')) {
                throw "Missing Feign URL: $url"
            }
        } elseif ($merged.ContainsKey('job-service.url') -or $merged.ContainsKey('candidate-service.url')) {
            throw "Application-service config leaked into $service"
        }
        $filePattern = '*/' + $service + '-' + $profile + '.yml'
        if (-not ($response.propertySources | Where-Object { $_.name -like $filePattern })) {
            throw "Missing service/profile source: $url"
        }
        [pscustomobject]@{
            Service = $service
            Profile = $profile
            Result = 'PASS'
            Message = $merged['ats.demo.message']
            Commit = $response.version
        }
    }
}
$rows | Format-Table -AutoSize
Write-Host 'PASS: health + 9 service/profile combinations; config values, source files and isolation verified.'
