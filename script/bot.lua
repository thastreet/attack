socket = require "socket"
json = require "json"
host, port = "127.0.0.1", 9876

sid = socket.udp()
sid:settimeout(1 / 60)

serverResponse = nil

local function HandleServerResponse()
    if serverResponse == "left" then
        joypad.set(1, {left = true})
    elseif serverResponse == "right" then
        joypad.set(1, {right = true})
    elseif serverResponse == "up" then
        joypad.set(1, {up = true})
    elseif serverResponse == "down" then
        joypad.set(1, {down = true})
    elseif serverResponse == "A" then
        joypad.set(1, {A = true})
    end

    emu.registerbefore(nil)
end

local function AfterFrame()
    board = {}
    topLeftAddress = 0x007E0FBE

    for j = 0,11,1 
    do 
        row = {}
        for i = 0,5,1 
        do 
            valueAddressInt = topLeftAddress + i * 2 + j * 16
            valueAddress = string.format("0x00%X", valueAddressInt)
            stateAddress = string.format("0x00%X", valueAddressInt + 1)

            value = memory.readbyte(valueAddress)
            state = memory.readbyte(stateAddress)

            table.insert(row, { value = value, state = state })
        end

        table.insert(board, row)
    end

    cursor = { x = memory.readbyte(0x007E03A4), y = memory.readbyte(0x007E03B0) }

    sid:sendto( json.encode({ board = board, cursor = cursor }), host, port )

    serverResponse = sid:receive()

    if serverResponse then
        print("Received: ", serverResponse)
        emu.registerbefore(HandleServerResponse)
    end
end
emu.registerafter(AfterFrame)