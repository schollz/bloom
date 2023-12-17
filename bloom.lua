-- bloom v0.0.1
-- (adapted from Eno)
--
-- llllllll.co/t/bloom
--
-- 
-- 
--    ▼ instructions below ▼
--
--
--

musicutil=require("musicutil")
ggrid_=include("lib/ggrid")
-- check for requirements
installer_=include("lib/scinstaller/scinstaller")
installer=installer_:new{requirements={"Fverb"},zip="TODO"}
engine.name=installer:ready() and 'Bloom' or nil

CURSOR_DEBOUNCE = 150
cursor={x=64,y=32,r=1,angle=180,moved=CURSOR_DEBOUNCE,show=false}

function add_circle(c)
  table.insert(ggrid.circles,c)
end

function init()
  if not installer:ready() then
    clock.run(function()
      while true do
        redraw()
        clock.sleep(1/5)
      end
    end)
    do return end
  end
  ggrid=ggrid_:new{add_circle=add_circle}
  ggrid.circles = {
    {x=64,y=32,r=1,l=15},
  }

  -- setup osc
  osc_fun={
    emit=function(args)
      local x = tonumber(args[1])
      local y = tonumber(args[2])
      local l = util.linlin(0,180,15,1,tonumber(args[#args]))
      add_circle({x=x*128,y=y*64,r=0,l=l})
    end,
  }
  osc.event=function(path,args,from)
    if string.sub(path,1,1)=="/" then
      path=string.sub(path,2)
    end
    if path~=nil and osc_fun[path]~=nil then
      osc_fun[path](args)
    else
      -- print("osc.event: '"..path.."' ?")
    end
  end

  local bloom_scales={
    "ambrette",
    "benzoin",
    "bergamot",
    "labdanum",
    "neroli",
    "orris",
    "tolu",
    "vetiver",
    "ylang"
  }
  params:add_option("scale","scale",bloom_scales)
  params:set_action("scale",function(v)
    engine.setScale(params:string("scale"))
  end)
  params:add_number(
    "duration", -- id
    "pattern duration", -- name
    1, -- min
    600, -- max
    60, -- default
    function(param) return string.format("%d sec",param:get()) end -- formatter
  )
  params:set_action("duration",function(v)
    engine.setPatternDuration(params:get("duration"))
  end)
  params:add_number(
    "seconds_between", -- id
    "after recording", -- name
    1, -- min
    10, -- max
    2, -- default
    function(param) return string.format("%d sec",param:get()) end -- formatter
  )
  params:set_action("seconds_between",function(v)
    engine.setSecondsBetweenRecordings(params:get("seconds_between"))
  end)


  params:bang()
  redraw()
end

function cleanup()
  params:set("monitor_level",current_monitor_level)
end

function key(k,z)
  if not installer:ready() then
    installer:key(k,z)
    do return end
  end
end

function enc(k,d)
  if not installer:ready() then
    do return end
  end
  if k==1 then
    cursor.moved=CURSOR_DEBOUNCE
    cursor.x = math.random(1,128)
    cursor.y = math.random(1,64) 
  elseif k==2 then
    cursor.moved=CURSOR_DEBOUNCE
    cursor.x = util.wrap(cursor.x+d,1,128)
  elseif k==3 then
    cursor.moved=CURSOR_DEBOUNCE
    cursor.y = util.wrap(cursor.y-d,1,64)
  end
end

function key(k,z)
  if k==3 and z==1 then 
    local x = cursor.x / 128
    local y = cursor.y / 64
    engine.record(x,y)
    self.add_circle({x=x*128,y=y*64,r=0,l=15})
  end

end


function update_circles()
  local new_circles = {}
  local toremove = {}
  for i,c in ipairs(ggrid.circles) do 
    c.r = c.r + 0.35
    c.l = c.l - (c.l/52)
    screen.level(util.round(c.l))
    screen.circle(util.round(c.x),util.round(c.y),util.round(c.r)) 
    screen.fill()

    if c.r < 80 and c.l>0.25 then
      table.insert(new_circles,{x=c.x,y=c.y,r=c.r,l=c.l,visual=c.visual,rf=c.rd})
    end
  end
  ggrid.circles = new_circles
end

function refresh()
	redraw()
end

function redraw()
  if not installer:ready() then
    installer:redraw()
    do return end
  end
  screen.clear()
  screen.blend_mode(2)

  update_circles()

  if cursor.show then
    screen.move(cursor.x,cursor.y)
    screen.level(util.linlin(0,CURSOR_DEBOUNCE,0,15,cursor.moved))
    screen.text_center("+")
  end

  screen.update()
end

