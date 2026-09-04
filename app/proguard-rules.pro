# Add project specific ProGuard rules here.
# The project has minify disabled by default, so these rules are mainly
# for future reference if shrinking is enabled.

# Keep the MIDI service bindings
-keep class android.media.midi.** { *; }
