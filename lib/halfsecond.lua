-- half sec loop 75% decay

local sc = {}

function sc.init()
  print("starting halfsecond")
audio.level_cut(1.0)
audio.level_adc_cut(1)
audio.level_eng_cut(1)

softcut.level_slew_time(1,0.25)
softcut.level_input_cut(1, 1, 1.0)
softcut.level_input_cut(2, 2, 1.0)
softcut.pan(1, -1)
softcut.pan(2, 1)
for i=1,2 do 
	softcut.level(i,1.0)
	softcut.play(i, 1)
	softcut.rate(i, 1)
	softcut.rate_slew_time(i,0.25)
	softcut.loop_start(i, 1+i*10)
	softcut.loop_end(i, 1.5+i*10)
	softcut.loop(i, 1)
	softcut.fade_time(i, 0.1)
	softcut.rec(i, 1)
	softcut.rec_level(i, 1)
	softcut.pre_level(i, 0.75)
	softcut.position(i, 1)
	softcut.enable(i, 1)
	
	softcut.filter_dry(i, 0.125);
	softcut.filter_fc(i, 1200);
	softcut.filter_lp(i, 0);
	softcut.filter_bp(i, 1.0);
	softcut.filter_rq(i, 2.0);
	
	
end
  params:add_group("DELAY",3)
  params:add{id="delay", name="delay", type="control", 
    controlspec=controlspec.new(0,1,'lin',0,0.5,""),
    action=function(x) softcut.level(1,x*math.random(90,110)/100);softcut.level(2,x*math.random(90,110)/100) end}
  params:add{id="delay_rate", name="delay rate", type="control", 
    controlspec=controlspec.new(0.5,2.0,'lin',0,1,""),
    action=function(x) softcut.rate(1,x*math.random(90,110)/100); softcut.rate(2,x*math.random(90,110)/100) end}
  params:add{id="delay_feedback", name="delay feedback", type="control", 
    controlspec=controlspec.new(0,1.0,'lin',0,0.75,""),
    action=function(x) softcut.pre_level(1,x*math.random(90,110)/100); softcut.pre_level(2,x*math.random(90,110)/100) end}
end

return sc
