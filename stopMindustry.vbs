for each process in getObject("winmgmts:\\.\root\cimv2").ExecQuery("select * from Win32_Process")
    if process.name = "javaw.exe" and inStr(process.commandLine, "Mindustry.jar") then process.terminate()
    else if process.name = "Mindustry.jar" then process.terminate()
next