# Usage: python3 tools/make_sounds.py <folder for WAVs>, then convert each to OGG into app/src/main/res/raw, e.g.
#   for f in <folder>/*.wav; do ffmpeg -y -i "$f" -c:a libvorbis -q:a 2 "app/src/main/res/raw/$(basename "$f" .wav).ogg"; done
# (the ambient track uses -q:a 1 to keep it small)
# Makes the Nova Club sounds from scratch: a quiet, looping ambient track and a set of soft UI sound effects.
# Writes WAV files; ffmpeg then turns them into small OGG files for res/raw.
import numpy as np, wave, os, sys

OUT = sys.argv[1]
SR = 44100
rng = np.random.default_rng(7)

def note(name):
    # e.g. "F3" -> Hz
    names = {"C": 0, "Db": 1, "D": 2, "Eb": 3, "E": 4, "F": 5, "Gb": 6, "G": 7, "Ab": 8, "A": 9, "Bb": 10, "B": 11}
    pitch, octave = name[:-1], int(name[-1])
    midi = 12 * (octave + 1) + names[pitch]
    return 440.0 * 2 ** ((midi - 69) / 12)

def lowpass(x, cutoff, slope=2.0):
    # gentle lowpass in the frequency domain (whole signal at once)
    X = np.fft.rfft(x, axis=0)
    f = np.fft.rfftfreq(x.shape[0], 1 / SR)
    gain = 1 / np.sqrt(1 + (f / cutoff) ** (2 * slope))
    return np.fft.irfft(X * (gain[:, None] if x.ndim == 2 else gain), n=x.shape[0], axis=0)

def highpass(x, cutoff):
    X = np.fft.rfft(x, axis=0)
    f = np.fft.rfftfreq(x.shape[0], 1 / SR)
    gain = 1 / np.sqrt(1 + (cutoff / np.maximum(f, 1e-3)) ** 4)
    return np.fft.irfft(X * (gain[:, None] if x.ndim == 2 else gain), n=x.shape[0], axis=0)

def reverb(x, seconds=4.0, wet=0.5, seed=1, bright=3500):
    # convolution with a decaying stereo noise tail (a soft, spacious room)
    r = np.random.default_rng(seed)
    n = int(seconds * SR)
    t = np.arange(n) / SR
    env = np.exp(-t * 6.9 / seconds)
    ir = np.stack([r.standard_normal(n) * env, r.standard_normal(n) * env], axis=1)
    ir = lowpass(ir, bright)
    ir[:int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))[:, None]   # a short gap before the reflections
    ir /= np.sqrt((ir ** 2).sum(axis=0))
    if x.ndim == 1:
        x = np.stack([x, x], axis=1)
    m = x.shape[0] + n
    size = 1 << (m - 1).bit_length()
    out = np.zeros((m, 2))
    for ch in range(2):
        out[:, ch] = np.fft.irfft(np.fft.rfft(x[:, ch], size) * np.fft.rfft(ir[:, ch], size), size)[:m]
    dry = np.zeros((m, 2)); dry[:x.shape[0]] = x
    return dry * (1 - wet) + out * wet * 3.0

def write(name, x, peak=0.89):
    if x.ndim == 1:
        x = x[:, None]
    x = x / (np.abs(x).max() + 1e-9) * peak
    data = (x * 32767).astype(np.int16)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(data.shape[1]); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(data.tobytes())

# ---------------------------------------------------------------- AMBIENT
# Eight chords, eight seconds each (64 s), in F minor with a neo-soul colour, looping seamlessly.
CHORDS = [
    ("F2",  ["Ab3", "C4", "Eb4", "G4"]),        # Fm9
    ("Db2", ["F3", "Ab3", "C4", "Eb4"]),        # Dbmaj9
    ("Bb1", ["Db3", "F3", "Ab3", "C4"]),        # Bbm9
    ("C2",  ["Eb3", "G3", "Bb3", "D4"]),        # Cm9
    ("F2",  ["Ab3", "C4", "Eb4", "G4"]),        # Fm9
    ("Eb2", ["G3", "Bb3", "D4", "F4"]),         # Ebmaj9
    ("Db2", ["F3", "C4", "Eb4", "G4"]),         # Dbmaj9#11
    ("C2",  ["F3", "Bb3", "E4", "G4"]),         # C7sus, leaning home
]
CHORD_S = 8.0
LOOP_S = CHORD_S * len(CHORDS)
TAIL_S = 7.0
N = int((LOOP_S + TAIL_S) * SR)
mix = np.zeros((N, 2))

def pad_note(freq, length, detune_cents, pan):
    t = np.arange(int(length * SR)) / SR
    sig = np.zeros_like(t)
    for d in (-detune_cents, 0, detune_cents):               # three slightly detuned voices
        f = freq * 2 ** (d / 1200)
        phase = rng.uniform(0, 2 * np.pi)
        h = 1
        while f * h < 2600 and h <= 14:
            sig += np.sin(2 * np.pi * f * h * t + phase * h) / (h ** 1.6)
            h += 1
    # slow swell in, long fade out
    att, rel = 2.6, 3.4
    env = np.minimum(1, t / att) * np.minimum(1, np.maximum(0, (length - t) / rel))
    env = env ** 1.5
    trem = 1 + 0.08 * np.sin(2 * np.pi * 0.13 * t + rng.uniform(0, 6))   # a gentle breathing
    s = sig * env * trem
    return np.stack([s * (1 - pan), s * (1 + pan)], axis=1) * 0.5

for i, (root, tones) in enumerate(CHORDS):
    start = int(i * CHORD_S * SR)
    length = CHORD_S + 2.5                                  # overlaps into the next chord for a smooth change
    for k, tone in enumerate(tones):
        v = pad_note(note(tone), length, 7, pan=(k - 1.5) * 0.25)
        end = min(N, start + v.shape[0]); mix[start:end] += v[:end - start] * 0.55
    # a soft sub under the root
    t = np.arange(int(length * SR)) / SR
    sub = np.sin(2 * np.pi * note(root) * t) * np.minimum(1, t / 2.5) * np.minimum(1, np.maximum(0, (length - t) / 3.0))
    end = min(N, start + sub.shape[0]); mix[start:end] += np.stack([sub, sub], 1)[:end - start] * 0.55

# Twinkling "stars": soft bell notes from the chord, two octaves up, at random moments
for i, (root, tones) in enumerate(CHORDS):
    for _ in range(3):
        at = i * CHORD_S + rng.uniform(0.6, CHORD_S - 0.5)
        f = note(tones[rng.integers(len(tones))]) * 4
        t = np.arange(int(3.5 * SR)) / SR
        bell = (np.sin(2 * np.pi * f * t) + 0.35 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 3)) * np.exp(-t * 1.6)
        bell *= np.minimum(1, t / 0.004)
        pan = rng.uniform(-0.8, 0.8)
        s = int(at * SR); e = min(N, s + bell.shape[0])
        mix[s:e] += np.stack([bell * (1 - pan), bell * (1 + pan)], 1)[:e - s] * 0.07

# Space air and a little vinyl dust
air = lowpass(highpass(rng.standard_normal((N, 2)), 300), 2200) * 0.018
air *= (1 + 0.5 * np.sin(2 * np.pi * np.arange(N)[:, None] / SR * 0.05))
dust = np.zeros((N, 2))
for _ in range(int(LOOP_S * 3)):
    p = rng.integers(0, N - 200); dust[p:p + 40] += rng.standard_normal((40, 2)) * np.exp(-np.arange(40) / 6)[:, None] * 0.03
mix += air + lowpass(dust, 5000)

mix = lowpass(mix, 2400, slope=1.5)                       # warm, a little dark
wet = reverb(mix, seconds=5.0, wet=0.55, seed=3, bright=3000)
# Loop seamlessly: fold everything past the loop point back onto the start
loop_n = int(LOOP_S * SR)
amb = wet[:loop_n].copy()
over = wet[loop_n:]
amb[:over.shape[0]] += over[:loop_n]
write("ambient_nova", amb, peak=0.7)

# ---------------------------------------------------------------- SOUND EFFECTS
def tone(freq, seconds, decay, partials=((1, 1.0),), attack=0.003):
    t = np.arange(int(seconds * SR)) / SR
    s = sum(a * np.sin(2 * np.pi * freq * r * t) for r, a in partials)
    return s * np.exp(-t * decay) * np.minimum(1, t / attack)

def place(buf, sig, at):
    s = int(at * SR); e = min(buf.shape[0], s + sig.shape[0]); buf[s:e] += sig[:e - s]

GLASS = ((1, 1.0), (2.01, 0.35), (3.98, 0.12))
BELL = ((1, 1.0), (2.76, 0.4), (5.4, 0.15))

# A soft glassy tap
tap = tone(2400, 0.12, 60, GLASS) * 0.6 + tone(3600, 0.12, 90) * 0.2
write("sfx_tap", reverb(tap, 0.6, wet=0.2, seed=5), peak=0.5)

# A switch between areas: an airy little whoosh with a rising glint
n = int(0.45 * SR); t = np.arange(n) / SR
whoosh = highpass(lowpass(rng.standard_normal(n), 2500), 600) * np.sin(np.pi * np.minimum(1, t / 0.3)) ** 2 * 0.4
glide = np.sin(2 * np.pi * (700 * t + 400 * t * t)) * np.exp(-t * 9) * np.minimum(1, t / 0.01) * 0.5
write("sfx_switch", reverb(whoosh + glide, 1.0, wet=0.3, seed=6), peak=0.5)

# Hours up and down: little marimba-like steps
write("sfx_step_up", reverb(tone(note("C6"), 0.3, 18, ((1, 1), (4, 0.15))), 0.6, wet=0.2, seed=7), peak=0.5)
write("sfx_step_down", reverb(tone(note("Ab5"), 0.3, 18, ((1, 1), (4, 0.15))), 0.6, wet=0.2, seed=8), peak=0.5)

# Switches: two quick notes, up for on and down for off
on = np.zeros(int(0.4 * SR)); place(on, tone(note("Eb6"), 0.25, 25, GLASS), 0); place(on, tone(note("Ab6"), 0.3, 20, GLASS), 0.07)
off = np.zeros(int(0.4 * SR)); place(off, tone(note("Ab6"), 0.25, 25, GLASS), 0); place(off, tone(note("Eb6"), 0.3, 20, GLASS), 0.07)
write("sfx_toggle_on", reverb(on, 0.7, wet=0.25, seed=9), peak=0.5)
write("sfx_toggle_off", reverb(off, 0.7, wet=0.25, seed=10), peak=0.5)

# A booking lands: a comet whooshing in, then a shimmering Fmaj9 bell chord
n = int(2.6 * SR); landing = np.zeros(n); t = np.arange(int(0.6 * SR)) / SR
comet = highpass(lowpass(rng.standard_normal(t.shape[0]), 6000), 1500) * (t / 0.6) ** 2 * 0.5
place(landing, comet, 0)
for k, nm in enumerate(["F5", "A5", "C6", "E6", "G6"]):
    place(landing, tone(note(nm), 2.0, 2.2, BELL) * (0.55 - k * 0.05), 0.55 + k * 0.045)
write("sfx_booking_in", reverb(landing, 2.5, wet=0.45, seed=11), peak=0.6)

# A booking frees up: stardust, high little plinks falling and fading
n = int(2.2 * SR); dust = np.zeros(n)
for k in range(14):
    f = note("C7") * 2 ** (-(k * 0.6 + rng.uniform(0, 0.4)) / 12 * 2)
    place(dust, tone(f, 0.6, 9, GLASS) * (0.5 * (1 - k / 16)), k * 0.07 + rng.uniform(0, 0.03))
write("sfx_booking_out", reverb(dust, 2.0, wet=0.45, seed=12), peak=0.55)

# Still free: a warm two-note chime up
ok = np.zeros(int(1.6 * SR)); place(ok, tone(note("Ab5"), 1.2, 3, BELL) * 0.6, 0); place(ok, tone(note("Eb6"), 1.3, 3, BELL) * 0.6, 0.12)
write("sfx_confirm", reverb(ok, 1.8, wet=0.4, seed=13), peak=0.55)

# Taken: a soft, low, muffled bump
t = np.arange(int(0.35 * SR)) / SR
bump = np.sin(2 * np.pi * (190 * t - 120 * t * t)) * np.exp(-t * 14) * np.minimum(1, t / 0.004)
write("sfx_clash", reverb(lowpass(bump, 900), 0.6, wet=0.2, seed=14), peak=0.5)
print("ok")
