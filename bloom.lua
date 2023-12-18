-- bloom v0.0.1
-- (adapted from Eno)
--
-- llllllll.co/t/bloom
--
--
--
--    ▼ instructions below ▼
--
-- E1: change scale
-- K2: toggle rand/generate
-- K3: generate a new pattern
--

musicutil=require("musicutil")
ggrid_=include("lib/ggrid")
halfsecond=include("lib/halfsecond")
engine.name="Bloom"

CURSOR_DEBOUNCE=150
cursor={x=64,y=32,r=1,angle=180,moved=CURSOR_DEBOUNCE,show=false}

function add_circle(c)
  table.insert(ggrid.circles,c)
end

function init()
  ggrid=ggrid_:new{add_circle=add_circle}
  ggrid.circles={
    {x=64,y=32,r=1,l=15},
  }
  halfsecond.init()

  -- setup osc
  osc_fun={
    emit=function(args)
      local x=tonumber(args[1])
      local y=tonumber(args[2])
      local l=util.linlin(0,180,15,1,tonumber(args[#args]))
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

  params:add_option("generate","generate",{"off","on"},1)
  params:add_option("randomize","randomize",{"off","on"},1)

  bloom_scales={
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
  params:add_option("scale","scale",bloom_scales,6)
  params:set_action("scale",function(v)
    engine.setScale(params:string("scale"))
  end)

  params:add{
    type="control",
    id="delay",
    name="delay",
    controlspec=controlspec.new(0.1,10,"lin",0.1,3.1,"s",1/100),
    action=function(x) engine.setSecondsBetweenPatterns(x) end
  }

  params:add{
    type="control",
    id="blend",
    name="blend",
    controlspec=controlspec.new(0,1,"lin",0.01,0.02,"",0.01/1),
    action=function(x) engine.setBlend(x) end
  }

  params:add_number(
    "duration",-- id
    "duration",-- name
    1,-- min
    600,-- max
    60,-- default
    function(param) return string.format("%d sec",param:get()) end -- formatter
  )
  params:set_action("duration",function(v)
    engine.setPatternDuration(params:get("duration"))
  end)
 
  params:add{
    type="control",
    id="seconds_between",
    name="recording delay",
    controlspec=controlspec.new(1,10,"lin",0.1,2.1,"sec",0.1/10),
    action=function(x)
    engine.setSecondsBetweenRecordings(params:get("seconds_between"))
    end
  }

  params:add_number(
    "drone_volume",-- id
    "drone volume",-- name
    -96,-- min
    6,-- max
    0,-- default
    function(param) return string.format("%d dB",param:get()) end -- formatter
  )
  params:set_action("drone_volume",function(v)
    engine.setDroneVolume(params:get("drone_volume"))
  end)

  params:bang()
  redraw()

  local generate_debounce=10
  clock.run(function()
    while true do
      clock.sleep(1)
      if params:get("randomize")==2 then
        if math.random(1,100)<10 then
          params:set("scale",math.random(1,#bloom_scales))
        end
        if math.random(1,100)<10 then
          params:set("delay",math.random(20,60)/10)
        end
        if math.random(1,100)<10 then
          params:set("blend",math.random(0,100)/100)
        end
      end
      if params:get("generate")==2 then
        if generate_debounce>0 then
          generate_debounce=generate_debounce-1
        end
        if generate_debounce==0 and math.random(1,100)<10 then
          print("generating")
          local num_positions=math.random(3,10)
          for i=1,num_positions do
            local x=math.random(1,128)/128
            local y=math.random(1,64)/64
            print(i,x,y)
            engine.record(x,y)
            add_circle({x=x*128,y=y*64,r=0,l=15})
            clock.sleep(math.random(10,1000)/1000)
          end
          generate_debounce=math.random(3,10)
        end
      end
    end
  end)
end

function enc(k,d)
  if k==1 then
    params:delta("scale",d)
  end
end

function key(k,z)
  if k==3 and z==1 then
    cursor.x=math.random(1,128)
    cursor.y=math.random(1,64)
    local x=cursor.x/128
    local y=cursor.y/64
    engine.record(x,y)
    add_circle({x=x*128,y=y*64,r=0,l=15})
  elseif k==2 and z==1 then
    if params:get("randomize")==2 then
      if params:get("generate")==2 then
        params:set("randomize",1)
      else
        params:set("generate",2)
      end
    elseif params:get("generate")==2 then
      params:set("generate",1)
    else
      params:set("randomize",2)
    end
  end
end

function update_circles()
  local new_circles={}
  local toremove={}
  for i,c in ipairs(ggrid.circles) do
    c.r=c.r+0.35
    c.l=c.l-(c.l/52)
    screen.level(util.round(c.l))
    screen.circle(util.round(c.x),util.round(c.y),util.round(c.r))
    screen.fill()

    if c.r<100 and c.l>0.002 then
      table.insert(new_circles,{x=c.x,y=c.y,r=c.r,l=c.l,visual=c.visual,rf=c.rd})
    end
  end
  ggrid.circles=new_circles
end

function refresh()
  redraw()
end

function redraw()
  screen.clear()
  screen.blend_mode(2)

  update_circles()

  if cursor.show then
    screen.move(cursor.x,cursor.y)
    screen.level(util.linlin(0,CURSOR_DEBOUNCE,0,15,cursor.moved))
    screen.text_center("+")
  end

  screen.level(5)
  screen.move(2,8)
  screen.text(params:string("scale"))
  screen.move(128,8)
  if params:get("generate")==2 then
    if params:get("randomize")==2 then
      screen.text_right("generate+randomize")
    else
      screen.text_right("generate")
    end
  elseif params:get("randomize")==2 then
    screen.text_right("randomize")
  end

  screen.update()
end

