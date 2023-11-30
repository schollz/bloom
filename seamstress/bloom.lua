function init()
  clock_redraw=clock.run(function()
    while true do
      clock.sleep(1/30)
      redraw()
    end
  end)
end

function redraw()
  local screen_w,screen_h=screen.get_size()
  screen.clear()
  screen.move(32,32)
  screen.color(250,250,250)
  screen.circle_fill(10)
  screen.render_texture_extended(2)

  screen.refresh()
end
