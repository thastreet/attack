socket = require "socket"
json = require "json"
host, port = "127.0.0.1", 9876

sid = socket.udp()

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

    sid:sendto( json.encode({ board = board }), host, port )
end
emu.registerafter(AfterFrame)