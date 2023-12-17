-- bloom v0.0.1
-- bloom clone
--
-- llllllll.co/t/bloom
--
-- originally created by
-- eno + TODO
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
end


function update_circles()
  local new_circles = {}
  local toremove = {}
  for i,c in ipairs(ggrid.circles) do 
    c.r = c.r + 0.4
    c.l = c.l - (c.l/50)
    screen.level(util.round(c.l))
    screen.circle(util.round(c.x),util.round(c.y),util.round(c.r)) 
    screen.fill()

    if c.r < 96 and c.l>0.25 then
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

  screen.update()
end

