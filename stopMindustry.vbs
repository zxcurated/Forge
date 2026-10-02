Set shell = CreateObject("WScript.Shell")

shell.Exec("taskkill /im Mindustry.exe")

set wmi = getObject("winmgmts:\\.\root\cimv2")

for each process in wmi.ExecQuery("select * from Win32_Process")
    if process.name = "javaw.exe" and inStr(process.commandLine, "Mindustry.jar") then process.terminate()
next