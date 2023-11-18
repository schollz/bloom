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

-- check for requirements
installer_=include("lib/scinstaller/scinstaller")
installer=installer_:new{requirements={"Fverb","BloomWildly"},zip="TODO"}
engine.name=installer:ready() and 'Bloom' or nil

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

  current_monitor_level=params:get("monitor_level")
  params:set("monitor_level",-99)

  local params_menu={
    {stage=0,id="preamp",name="preamp",min=-96,max=24,exp=false,div=0.1,default=0,formatter=function(param) local v=param:get()>0 and "+" or "";return string.format("%s%2.1f dB",v,param:get()) end},
  }
  for _,pram in ipairs(params_menu) do
    local id=pram.id..pram.stage
    -- if pram.id=="toggle" then
    --   params:add_separator(pram.name)
    -- end
    local name=pram.name
    if pram.id=="toggle" then
      name=string.upper(name).." >"
    end
    params:add{
      type="control",
      id=id,
      name=name,
      controlspec=controlspec.new(pram.min,pram.max,pram.exp and "exp" or "lin",pram.div,pram.default,pram.unit or "",pram.div/(pram.max-pram.min)),
      formatter=pram.formatter,
    }
    params:set_action(id,function(x)
      if pram.id=="toggle" then
        engine.toggle(pram.stage,x)
        -- update the hiding/showing in the menu
        for stage=1,11 do
          if params:get("toggle"..stage)==1 then
            stages_toggled=stages_toggled+1
          end
          for _,p in ipairs(params_menu) do
            if p.stage==stage then
              if params:get("toggle"..p.stage)==0 and p.id~="toggle" then
                params:hide(p.id..p.stage)
              else
                params:show(p.id..p.stage)
              end
            end
          end
        end
        _menu.rebuild_params()
      else
        engine.set(pram.stage,pram.id,pram.val and pram.val(x) or x)
      end
    end)
  end

  params:bang()

  clock.run(function()
    while true do
      clock.sleep(1/10)
      redraw()
    end
  end)
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

function redraw()
  if not installer:ready() then
    installer:redraw()
    do return end
  end
  screen.clear()

  screen.update()
end

