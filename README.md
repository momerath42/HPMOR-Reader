A simple android app that reads Harry Potter and the Methods of Rationality by Eliezer Yudkowsky using android's text to speech system (which supports plugins).  It could be generalized to other uses, but it currently makes assumptions, not only about where to download chapters (the plain text feed at http://www.elsewhere.org/rationality/ ), but how to find the beginning and end of the actual chapter text.  HPMOR is the only source of text that isn't read by others (up-to-date) that I crave enough to listen to TTS, so I have no personal need to generalize the app.  It also already does everything I really care about, so I may never get around to the following todo list:

TODO:
1. pull the text out of the html in a paragraph-preserving manner and feed the TTS system a paragraph (sentence?) at a time (may actually improve the speech quality- untested)
2. cache downloads and make downloading and reading properly asynchronous with visual indicator
3. make the visual appearance of the text appealing for actual reading (text w/ 'spans'? custom view?)
4. improved navigation
 a. temporary pause of tts text-advance when scrolling is detected (currently jumps when you're trying to find your place- can just pause, but...)
 b. pause/unpause text/icon change
 c. better skip-to-chapter dialog?
 d. left/right swipe to skip chapters?
5. new-chapter detection/notification
 a. prevent skipping to non-existent chapters