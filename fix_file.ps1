$c = Get-Content 'c:\Codex\ZamerKP\app\src\main\java\ru\depo\zamerykp\ui\AppRoot.kt'
$part1 = $c[0..1368]
$part2 = $c[1417..($c.Length-1)]
$combined = $part1 + $part2
$combined | Set-Content 'c:\Codex\ZamerKP\app\src\main\java\ru\depo\zamerykp\ui\AppRoot.kt'
