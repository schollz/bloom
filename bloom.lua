-- bloom v1.0.1
-- (adapted from Eno)
--
-- llllllll.co/t/bloom
--
--
--
--    ▼ instructions below ▼
--
-- E1: change scale
-- E2: change delay
-- E3: change blend
-- K2: clear pattern
-- K3: generate pattern
--
musicutil = require("musicutil")
ggrid_ = include("lib/ggrid")
halfsecond = include("lib/halfsecond")
engine.name = 'Bloom'

debounce_delay = 0
debounce_blend = 0
debounce_scale = 0
CURSOR_DEBOUNCE = 150
cursor = {
    x = 64,
    y = 32,
    r = 1,
    angle = 180,
    moved = CURSOR_DEBOUNCE,
    show = false
}
time_since_last_note = 0
local is_refreshed = false

options = {}
options.OUT = {"none", "midi", "crow out 1+2", "crow ii JF", "crow ii 301"}
local midi_devices
local midi_device
local midi_channel
local active_notes = {}
function add_circle(c) table.insert(ggrid.circles, c) end

function init()
    ggrid = ggrid_:new{add_circle = add_circle}
    ggrid.circles = {{x = 64, y = 32, r = 1, l = 15}}

    midi_devices = {}
    for i = 1, #midi.vports do
        local long_name = midi.vports[i].name
        local short_name = string.len(long_name) > 15 and
                               util.acronym(long_name) or long_name
        table.insert(midi_devices, i .. ": " .. short_name)
    end

    -- setup osc
    osc_fun = {
        emit = function(args)
            local x = tonumber(args[1])
            local y = tonumber(args[2])
            local l = util.linlin(0, 180, 15, 1, tonumber(args[#args]))
            add_circle({x = x * 128, y = y * 64, r = 0, l = l})
        end,
        note_on_norns = function(args)
            do_note(tonumber(args[1]), tonumber(args[2]), true)
        end,
        note_off_norns = function(args)
            do_note(tonumber(args[1]), 0, false)
        end
    }
    osc.event = function(path, args, from)
        if string.sub(path, 1, 1) == "/" then path = string.sub(path, 2) end
        if path ~= nil and osc_fun[path] ~= nil then
            osc_fun[path](args)
        else
            -- print("osc.event: '"..path.."' ?")
        end
    end

    params:add_separator("BLOOM")

    halfsecond.init()

    params:add_group("outs", 3)
    params:add{
        type = "option",
        id = "out",
        name = "out",
        options = options.OUT,
        action = function(value)
            all_notes_off()
            if value == 3 then
                crow.output[2].action = "{to(5,0),to(0,0.25)}"
            elseif value == 4 or value == 5 then
                crow.ii.pullup(true)
                crow.ii.jf.mode(1)
            end
        end
    }
    params:add{
        type = "option",
        id = "midi_device",
        name = "midi out device",
        options = midi_devices,
        default = 1,
        action = function(value) midi_device = midi.connect(value) end
    }

    params:add{
        type = "number",
        id = "midi_out_channel",
        name = "midi out channel",
        min = 1,
        max = 16,
        default = 1,
        action = function(value)
            all_notes_off()
            midi_channel = value
        end
    }

    params:add_group("bells", 7)
    params:add{
        type = "control",
        id = "note_duration",
        name = "note duration",
        controlspec = controlspec.new(0, 200, "lin", 1, 100, "%", 1 / 200),
        action = function(x) engine.setBell("dur", x / 100) end
    }
    params:add{
        type = "control",
        id = "noise_level",
        name = "noise level",
        controlspec = controlspec.new(0, 200, "lin", 1, 15, "%", 1 / 200),
        action = function(x) engine.setBell("nl", x / 100) end
    }
    params:add{
        type = "control",
        id = "noise_attack",
        name = "noise attack",
        controlspec = controlspec.new(0, 2000, "lin", 1, 5, "ms", 1 / 2000),
        action = function(x) engine.setBell("natk", x / 1000) end
    }
    params:add{
        type = "control",
        id = "noise_release",
        name = "noise release",
        controlspec = controlspec.new(0, 400, "lin", 1, 100, "%", 1 / 400),
        action = function(x) engine.setBell("noiserelease", x / 100) end
    }
    params:add{
        type = "control",
        id = "synth_attack",
        name = "synth attack",
        controlspec = controlspec.new(0, 2000, "lin", 1, 5, "ms", 1 / 2000),
        action = function(x) engine.setBell("atk", x / 1000) end
    }
    params:add{
        type = "control",
        id = "synth_release",
        name = "synth release",
        controlspec = controlspec.new(0, 400, "lin", 1, 75, "%", 1 / 400),
        action = function(x) engine.setBell("release", x / 100) end
    }
    params:add{
        type = "control",
        id = "filter_level",
        name = "filter level",
        controlspec = controlspec.WIDEFREQ,
        action = function(x) engine.setBell("filt", x) end
    }
    params:set("filter_level", 5000);

    params:add_option("randomize", "randomize", {"off", "on"}, 1)
    params:add_option("evolve", "evolve when idle", {"off", "on"}, 2)

    bloom_scales = {
        "ambrette", "benzoin", "bergamot", "labdanum", "neroli", "orris",
        "tolu", "vetiver", "ylang"
    }
    params:add_option("scale", "scale", bloom_scales, 4)
    params:set_action("scale", function(v)
        engine.setScale(params:string("scale"))
        debounce_scale = 180
    end)

    params:add{
        type = "control",
        id = "delayEngine",
        name = "delay",
        controlspec = controlspec.new(0.1, 10, "lin", 0.1, 3.1, "s", 1 / 100),
        action = function(x)
            debounce_delay = 180
            engine.setSecondsBetweenPatterns(x)
        end
    }

    params:add{
        type = "control",
        id = "blend",
        name = "blend",
        controlspec = controlspec.new(0, 100, "lin", 1, 2, "%", 1 / 100),
        action = function(x)
            engine.setBlend(x / 100)
            debounce_blend = 180
        end
    }

    params:add_number("duration", -- id
    "duration", -- name
    1, -- min
    600, -- max
    60, -- default
    function(param) return string.format("%d sec", param:get()) end -- formatter
    )
    params:set_action("duration", function(v)
        engine.setPatternDuration(params:get("duration"))
    end)

    params:add{
        type = "control",
        id = "seconds_between",
        name = "recording delay",
        controlspec = controlspec.new(1, 10, "lin", 0.1, 2.1, "sec", 0.1 / 10),
        action = function(x)
            engine.setSecondsBetweenRecordings(params:get("seconds_between"))
        end
    }

    params:add{
        type = "number",
        id = "root_note",
        name = "root note",
        min = 0,
        max = 127,
        default = 60,
        formatter = function(param)
            return musicutil.note_num_to_name(param:get(), true)
        end,
        action = function() engine.setRoot(params:get("root_note") - 60) end
    }

    params:add_number("drone_volume", -- id
    "drone volume", -- name
    -96, -- min
    12, -- max
    -3, -- default
    function(param)
        local s = ""
        if param:get() > 0 then s = "+" end
        return string.format("%s%d dB", s, param:get())
    end -- formatter
    )
    params:set_action("drone_volume", function(v)
        engine.setDroneVolume(params:get("drone_volume"))
    end)

    params:add{
        type = "control",
        id = "shimmer",
        name = "shimmer",
        controlspec = controlspec.new(0, 100, "lin", 1, 2, "%", 1 / 100),
        action = function(x) engine.setShimmer(x / 100) end
    }

    local num_recorders_max = 8
    if ggrid ~= nil and ggrid.rows ~= nil then num_recorders_max = ggrid.rows end
    params:add_number("recorders", "lanes", 1, num_recorders_max, 1)

    params:bang()
    redraw()

    clock.run(function()
        while true do
            clock.sleep(1 / 60)
            update_circles()
            if not is_refreshed then redraw() end
            if debounce_blend > 0 then
                debounce_blend = debounce_blend - 1
            end
            if debounce_scale > 0 then
                debounce_scale = debounce_scale - 1
            end
            if debounce_delay > 0 then
                debounce_delay = debounce_delay - 1
            end
        end
    end)

    local generate_debounce = 10
    local do_generate = true
    clock.run(function()
        while true do
            clock.sleep(1)
            time_since_last_note = time_since_last_note + 1
            if params:get("randomize") == 2 then
                if math.random(1, 100) < 10 then
                    params:set("scale", math.random(1, #bloom_scales))
                end
                if math.random(1, 100) < 10 then
                    params:set("delayEngine", math.random(20, 60) / 10)
                end
                if math.random(1, 100) < 10 then
                    params:set("blend", math.random(0, 100) / 100)
                end
            end
            if params:get("evolve") == 2 then
                if generate_debounce > 0 then
                    generate_debounce = generate_debounce - 1
                end
                if (generate_debounce == 0 and math.random(1, 100) < 10) or
                    time_since_last_note > 16 then
                    print("[bloom] generating")
                    local num_positions = math.random(3, 10)
                    for i = 1, num_positions do
                        local x = math.random(1, 128) / 128
                        local y = math.random(1, 64) / 64
                        print(i, x, y)
                        engine.record(0, x, y)
                        add_circle({x = x * 128, y = y * 64, r = 0, l = 15})
                        clock.sleep(math.random(100, params:get(
                                                    "seconds_between") * 1000) /
                                        1000)
                    end
                    generate_debounce = math.random(6, 23)
                end
            end
        end
    end)
end

function all_notes_off()
    if params:get("out") == 2 then
        for _, a in pairs(active_notes) do
            midi_device:note_off(a, nil, midi_channel)
        end
    end
    active_notes = {}
end

function do_note(note_num, velocity, on)
    if on then time_since_last_note = 0 end
    if params:get("out") == 2 then
        if on then
            midi_device:note_on(note_num, velocity, midi_channel)
            table.insert(active_notes, note_num)
        else
            midi_device:note_off(note_num, nil, midi_channel)
        end

    elseif params:get("out") == 3 and on then
        crow.output[1].volts = (note_num - 60) / 12
        crow.output[2].execute()
    elseif params:get("out") == 4 and on then
        crow.ii.jf.play_note((note_num - 60) / 12, 5)
    elseif params:get("out") == 5 and on then -- er301
        crow.ii.er301.cv(1, (note_num - 60) / 12)
        crow.ii.er301.tr_pulse(1)
    end

end

function enc(k, d)
    if k == 1 then
        params:delta("scale", d)
    elseif k == 2 then
        params:delta("delayEngine", d)
    elseif k == 3 then
        params:delta("blend", d)
    end
end

function key(k, z)
    if k == 3 and z == 1 then
        cursor.x = math.random(1, 128)
        cursor.y = math.random(1, 64)
        local x = cursor.x / 128
        local y = cursor.y / 64
        engine.record(0, x, y)
        add_circle({x = x * 128, y = y * 64, r = 0, l = 15})
    elseif k == 2 and z == 1 then
        engine.removeAll()
    end
end

function update_circles()
    local new_circles = {}
    local toremove = {}
    for i, c in ipairs(ggrid.circles) do
        c.r = c.r + 0.35
        c.l = c.l - (c.l / 52)

        if c.r < 100 and c.l > 0.002 then
            table.insert(new_circles, {
                x = c.x,
                y = c.y,
                r = c.r,
                l = c.l,
                visual = c.visual,
                rf = c.rd
            })
        end
    end
    ggrid.circles = new_circles
end

function refresh()
    is_refreshed = true
    redraw()
end

function redraw()
    screen.clear()
    screen.blend_mode(2)

    if cursor.show then
        screen.move(cursor.x, cursor.y)
        screen.level(util.linlin(0, CURSOR_DEBOUNCE, 0, 15, cursor.moved))
        screen.text_center("+")
    end

    for i, c in ipairs(ggrid.circles) do
        screen.level(util.round(c.l))
        screen.circle(util.round(c.x), util.round(c.y), util.round(c.r))
        screen.fill()
    end

    if debounce_delay > 0 then
        screen.level(util.round(debounce_delay / 180 * 15))
        screen.move(1, 54 - 18)
        screen.text("delay " .. params:string("delayEngine"))
        screen.move(0, 60 - 18)
        screen.line(128, 60 - 18)
        screen.stroke()
        screen.move(0, 61 - 18)
        screen.line(128, 61 - 18)
        screen.stroke()
        screen.move(params:get("delayEngine") * 128 / 10, 56 - 18)
        screen.line(params:get("delayEngine") * 128 / 10, 64 - 18)
        screen.move(params:get("delayEngine") * 128 / 10 + 1, 56 - 18)
        screen.line(params:get("delayEngine") * 128 / 10 + 1, 64 - 18)
        screen.stroke()
    end
    if debounce_blend > 0 then
        screen.level(util.round(debounce_blend / 180 * 15))
        screen.move(1, 54)
        screen.text("blend " .. params:string("blend"))
        screen.move(0, 60)
        screen.line(128, 60)
        screen.stroke()
        screen.move(0, 61)
        screen.line(128, 61)
        screen.stroke()
        screen.move(params:get("blend") * 1.28, 56)
        screen.line(params:get("blend") * 1.28, 64)
        screen.move(params:get("blend") * 1.28 + 1, 56)
        screen.line(params:get("blend") * 1.28 + 1, 64)
        screen.stroke()
    end

    if (debounce_scale > 0) then
        screen.level(util.round(debounce_scale / 180 * 15))
        screen.move(2, 8)
        screen.text("scale: " .. params:string("scale"))
        screen.move(128, 8)
    end

    screen.update()
end

