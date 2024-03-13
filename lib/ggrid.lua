-- local pattern_time = require("pattern")
local GGrid = {}

function GGrid:new(args)
    local m = setmetatable({}, {__index = GGrid})
    local args = args == nil and {} or args

    m.add_circle = args.add_circle
    m.grid_on = args.grid_on == nil and true or args.grid_on

    -- initiate the grid
    m.g = grid.connect()
    m.g.key = function(x, y, z) if m.grid_on then m:grid_key(x, y, z) end end
    m.cols = m.g.cols
    m.rows = m.g.rows
    print("grid columns: " .. m.cols)
    print("grid rows: " .. m.rows)

    m.visual = {}
    for row = 1, m.rows do
        m.visual[row] = {}
        for col = 1, m.cols do m.visual[row][col] = 0 end
    end

    m.circles = {}

    -- keep track of pressed buttons
    m.pressed_buttons = {}

    -- grid refreshing
    m.grid_refresh = metro.init()
    m.grid_refresh.time = 1 / 60
    m.grid_refresh.event = function() if m.grid_on then m:grid_redraw() end end
    m.grid_refresh:start()

    return m
end

function GGrid:grid_key(x, y, z)
    self:key_press(y, x, z == 1)
    self:grid_redraw()
end

function GGrid:key_press(row, col, on)
    if on then
        self.pressed_buttons[row .. "," .. col] = true
    else
        self.pressed_buttons[row .. "," .. col] = nil
    end
    if on then
        local x = col / self.cols - 0.001
        local y = row / self.rows - 0.001
        local recorder = math.ceil(params:get("recorders") * y) - 1
        y = (y * params:get("recorders")) -
                math.floor(y * params:get("recorders"))
        print(recorder, x, y)
        engine.record(recorder, x, y)
        self.add_circle({x = x * 128, y = y * self.rows * 8, r = 0, l = 15})
    end
end

function GGrid:get_visual()
    -- clear visual
    for row = 1, self.rows do
        for col = 1, self.cols do
            if self.visual[row][col] > 0 then
                self.visual[row][col] = self.visual[row][col] -
                                            self.visual[row][col] / 80
                if self.visual[row][col] < 0 then
                    self.visual[row][col] = 0
                end
            end
        end
    end

    -- show the circles
    for i, c in ipairs(self.circles) do
        local col = util.round(c.x * self.cols / 128)
        local row = util.round(c.y * self.rows / 64)
        local cc = {}
        if self.circles[i].visual == nil then self.circles[i].visual = {} end
        if self.circles[i].rd == nil then self.circles[i].rd = {} end
        if (self.circles[i].rd[util.round(c.r)] == nil) then
            self.circles[i].rd[util.round(c.r)] = true
            for a = 0, 360, 6 do
                local x = util.round(c.x + c.r * math.cos(math.rad(a)))
                local y = util.round(c.y + c.r * math.sin(math.rad(a)))
                x = util.round(x * self.cols / 128)
                y = util.round(y / 8)
                if self.circles[i].visual[y] == nil then
                    self.circles[i].visual[y] = {}
                end
                if self.circles[i].visual[y][x] == nil and x > 0 and x <=
                    self.cols and y > 0 and y <= self.rows then
                    self.circles[i].visual[y][x] = true
                    self.visual[y][x] = self.visual[y][x] + c.l / 2
                    if (self.visual[y][x] > 15) then
                        self.visual[y][x] = 15
                    end
                end
            end
        end
    end

    -- illuminate currently pressed button
    for k, _ in pairs(self.pressed_buttons) do
        local row, col = k:match("(%d+),(%d+)")
        self.visual[tonumber(row)][tonumber(col)] = 15
    end

    return self.visual
end

function GGrid:grid_redraw()
    self.g:all(0)
    local gd = self:get_visual()
    local s = 1
    local e = self.cols
    local adj = 0
    for row = 1, self.rows do
        for col = s, e do
            if gd[row][col] ~= 0 then
                self.g:led(col + adj, row, util.round(gd[row][col]))
            end
        end
    end
    self.g:refresh()
end

return GGrid
