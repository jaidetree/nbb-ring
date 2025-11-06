# ClojureScript Ring HTTP Server

## Why

That's a reasonable question tbh, ultimately it comes down to 2 ideas:

1. I'm curious what a batteries included cljs web framework could look like.
   - There's sitefox, but that's more of a thin wrapper around express and the
     JS way of modeling this problemspace.
   - I've really liked the ring middleware approach and the simplicity of taking
     request hash-map and returning a respone hash-map.
   - In modern JS we're seeing sort of a turn towards that with libraries like
     HonoJS and the web server library the Remix devs made.
   - It would be fun to explore concepts like folder based routing, migration
     files that expose `(defn up [...])` and `(defn down [...])`, try
     leveraging my validator library to define table schemas and form validators.

2. I am hurting for a tool that introduces as few abstractions as possible.
   - Having used Phoenix in Elixir recently, coming back to the project after a
     month or so it feels like making trivial changes is a chore.
     - A whole lot of files need to be updated to accomplish anything useful.
     - Additionally, there's a lot of specific vocabulary that never quite
       seems to stick in my brain.
     - I can see how Phoenix out-of-the-box can take on large projects but I
       just don't find it enjoyable to hack on.
