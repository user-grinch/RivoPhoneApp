/* ==========================================================================
   Rivo Distribution Site — Expressive & Interactive Logic
   ========================================================================== */

(() => {
  "use strict";

  // ---------------------------------------------------------------------------
  // 1. Theme (Dark / Light) & Material You Accent Color Picker
  // ---------------------------------------------------------------------------
  const THEME_KEY = "rivo-theme";
  const ACCENT_KEY = "rivo-accent";
  const themeBtn = document.getElementById("theme-toggle");
  const sunIcon = document.getElementById("icon-sun");
  const moonIcon = document.getElementById("icon-moon");
  const paletteToggle = document.getElementById("palette-toggle");
  const paletteDropdown = document.getElementById("palette-dropdown");
  const swatches = Array.from(document.querySelectorAll(".swatch"));

  function applyTheme(theme) {
    const mode = theme === "dark" ? "dark" : "light";
    document.documentElement.setAttribute("data-theme", mode);
    if (themeBtn) {
      const label = mode === "dark" ? "Switch to light mode" : "Switch to dark mode";
      themeBtn.setAttribute("aria-label", label);
      themeBtn.setAttribute("title", label);
    }
    if (sunIcon && moonIcon) {
      sunIcon.style.display = mode === "dark" ? "block" : "none";
      moonIcon.style.display = mode === "dark" ? "none" : "block";
    }
    try {
      localStorage.setItem(THEME_KEY, mode);
    } catch (_) {}
  }

  function applyAccent(accent) {
    const valid = ["blue", "emerald", "purple", "coral", "amber"];
    const chosen = valid.includes(accent) ? accent : "blue";
    document.documentElement.setAttribute("data-accent", chosen);
    swatches.forEach((s) => s.classList.toggle("is-active", s.getAttribute("data-accent") === chosen));
    try {
      localStorage.setItem(ACCENT_KEY, chosen);
    } catch (_) {}
  }

  function initThemeAndAccent() {
    let savedTheme = null;
    let savedAccent = null;
    try {
      savedTheme = localStorage.getItem(THEME_KEY);
      savedAccent = localStorage.getItem(ACCENT_KEY);
    } catch (_) {}

    if (savedTheme === "light" || savedTheme === "dark") {
      applyTheme(savedTheme);
    } else if (window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches) {
      applyTheme("dark");
    } else {
      applyTheme("light");
    }

    applyAccent(savedAccent || "blue");

    themeBtn?.addEventListener("click", () => {
      const current = document.documentElement.getAttribute("data-theme");
      applyTheme(current === "dark" ? "light" : "dark");
    });

    paletteToggle?.addEventListener("click", (e) => {
      e.stopPropagation();
      const isOpen = paletteDropdown?.classList.toggle("is-open");
      paletteDropdown?.setAttribute("aria-hidden", String(!isOpen));
    });

    swatches.forEach((swatch) => {
      swatch.addEventListener("click", () => {
        const acc = swatch.getAttribute("data-accent");
        if (acc) applyAccent(acc);
      });
    });

    document.addEventListener("click", (e) => {
      if (paletteDropdown && !paletteDropdown.contains(e.target) && e.target !== paletteToggle) {
        paletteDropdown.classList.remove("is-open");
        paletteDropdown.setAttribute("aria-hidden", "true");
      }
    });
  }

  // ---------------------------------------------------------------------------
  // 2. Navigation & Mobile Menu
  // ---------------------------------------------------------------------------
  function initNav() {
    const toggle = document.getElementById("menu-toggle");
    const nav = document.getElementById("primary-nav");
    if (!toggle || !nav) return;

    toggle.addEventListener("click", () => {
      const open = nav.classList.toggle("open");
      toggle.setAttribute("aria-expanded", String(open));
    });

    nav.querySelectorAll("a").forEach((link) => {
      link.addEventListener("click", () => {
        nav.classList.remove("open");
        toggle.setAttribute("aria-expanded", "false");
      });
    });

    // Scroll Spy for Nav links
    const sections = Array.from(document.querySelectorAll("section[id]"));
    const navLinks = Array.from(nav.querySelectorAll("a[href^='#']"));

    window.addEventListener("scroll", () => {
      const scrollY = window.scrollY + 120;
      let currentId = "";
      sections.forEach((sec) => {
        if (scrollY >= sec.offsetTop && scrollY < sec.offsetTop + sec.offsetHeight) {
          currentId = sec.getAttribute("id");
        }
      });
      navLinks.forEach((link) => {
        const href = link.getAttribute("href");
        link.classList.toggle("is-active", href === `#${currentId}`);
      });
    }, { passive: true });
  }

  // ---------------------------------------------------------------------------
  // 3. Hero 3D Tilt Showcase
  // ---------------------------------------------------------------------------
  function initHeroShowcase() {
    const stage = document.getElementById("phone-stage");

    // 3D Parallax Tilt
    if (stage && window.matchMedia("(pointer: fine)").matches) {
      let isHovered = false;

      stage.addEventListener("mouseenter", () => { isHovered = true; });
      stage.addEventListener("mouseleave", () => {
        isHovered = false;
        stage.style.transform = "perspective(1200px) rotateX(0deg) rotateY(0deg)";
      });

      stage.addEventListener("mousemove", (e) => {
        if (!isHovered) return;
        const rect = stage.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;
        const rotX = (-y / (rect.height / 2)) * 9;
        const rotY = (x / (rect.width / 2)) * 9;
        stage.style.transform = `perspective(1200px) rotateX(${rotX.toFixed(2)}deg) rotateY(${rotY.toFixed(2)}deg)`;
      });
    }
  }

  // ---------------------------------------------------------------------------
  // 4. Interactive Live T9 Dialpad Playground
  // ---------------------------------------------------------------------------
  function initDialerDemo() {
    const input = document.getElementById("dialer-input");
    const bsBtn = document.getElementById("dialer-backspace");
    const keys = Array.from(document.querySelectorAll(".dial-key"));
    const matchesList = document.getElementById("matches-list");
    const matchesCount = document.getElementById("matches-count");
    const soundToggle = document.getElementById("sound-toggle");
    const soundOnIcon = document.getElementById("icon-sound-on");
    const soundOffIcon = document.getElementById("icon-sound-off");
    const simSelector = document.getElementById("sim-selector");
    const simLabel = document.getElementById("sim-label");
    const callBtn = document.getElementById("dialer-call-btn");
    const callModal = document.getElementById("call-sim-modal");
    const endCallBtn = document.getElementById("end-call-btn");
    const callAvatar = document.getElementById("sim-call-avatar");
    const callName = document.getElementById("sim-call-name");
    const callNumber = document.getElementById("sim-call-number");
    const callStatusText = document.getElementById("sim-call-status-text");

    let soundEnabled = true;
    let audioCtx = null;
    let currentSim = 1;

    // Sample contact records with real portraits and precomputed T9 mapping
    const CONTACTS = [
      { name: "Sophia Martinez", number: "+1 (212) 555-0198", t9: "767442", avatarImg: "assets/avatars/sophia.jpg", avatar: "S" },
      { name: "Alex Morgan", number: "+1 (415) 555-0142", t9: "2539", avatarImg: "assets/avatars/alex.jpg", avatar: "A" },
      { name: "Emma Watson", number: "+1 (312) 555-0177", t9: "3662", avatarImg: "assets/avatars/emma.jpg", avatar: "E" },
      { name: "Liam Chen", number: "+1 (650) 555-0123", t9: "5426", avatarImg: "assets/avatars/liam.jpg", avatar: "L" },
      { name: "Ava Johnson", number: "+1 (415) 555-0189", t9: "282", avatarImg: "assets/avatars/ava.jpg", avatar: "A" },
      { name: "Benjamin Clark", number: "+1 (555) 016-3351", t9: "2365", avatarImg: "assets/avatars/benjamin.jpg", avatar: "B" },
    ];

    // Standard DTMF frequency pairs for 0-9, *, #
    const DTMF_FREQS = {
      "1": [697, 1209], "2": [697, 1336], "3": [697, 1477],
      "4": [770, 1209], "5": [770, 1336], "6": [770, 1477],
      "7": [852, 1209], "8": [852, 1336], "9": [852, 1477],
      "*": [941, 1209], "0": [941, 1336], "#": [941, 1477],
    };

    function playTone(digit) {
      if (!soundEnabled) return;
      try {
        if (!audioCtx) {
          const AudioContextClass = window.AudioContext || window.webkitAudioContext;
          if (AudioContextClass) audioCtx = new AudioContextClass();
        }
        if (!audioCtx) return;
        if (audioCtx.state === "suspended") audioCtx.resume();

        const freqs = DTMF_FREQS[digit] || [700, 1200];
        const now = audioCtx.currentTime;

        const osc1 = audioCtx.createOscillator();
        const osc2 = audioCtx.createOscillator();
        const gain = audioCtx.createGain();

        osc1.frequency.value = freqs[0];
        osc2.frequency.value = freqs[1];
        gain.gain.setValueAtTime(0.08, now);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.12);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(audioCtx.destination);

        osc1.start(now);
        osc2.start(now);
        osc1.stop(now + 0.12);
        osc2.stop(now + 0.12);
      } catch (_) {}
    }

    soundToggle?.addEventListener("click", () => {
      soundEnabled = !soundEnabled;
      if (soundOnIcon && soundOffIcon) {
        soundOnIcon.style.display = soundEnabled ? "block" : "none";
        soundOffIcon.style.display = soundEnabled ? "none" : "block";
      }
      soundToggle.setAttribute("title", `Audio feedback: ${soundEnabled ? "ON" : "OFF"}`);
    });

    simSelector?.addEventListener("click", () => {
      currentSim = currentSim === 1 ? 2 : 1;
      if (simLabel) {
        simLabel.textContent = currentSim === 1 ? "SIM 1 · Primary" : "SIM 2 · Business";
      }
    });

    function renderMatches(query) {
      if (!matchesList) return;
      matchesList.innerHTML = "";
      const q = (query || "").trim();

      let matched = [];
      if (!q) {
        matched = CONTACTS;
      } else {
        matched = CONTACTS.filter((c) => {
          const rawNum = c.number.replace(/\D/g, "");
          return c.t9.includes(q) || rawNum.includes(q);
        });
      }

      if (matchesCount) {
        matchesCount.textContent = `${matched.length} ${matched.length === 1 ? "Match" : "Matches"}`;
      }

      if (matched.length === 0) {
        const empty = document.createElement("div");
        empty.className = "match-item";
        empty.innerHTML = `
          <div class="match-item-left">
            <div class="match-avatar">?</div>
            <div class="match-info">
              <strong>Unsaved Number</strong>
              <span>Tap call to dial ${q}</span>
            </div>
          </div>
        `;
        empty.addEventListener("click", () => startSimulatedCall({ name: "Direct Dial", number: q, avatar: "📞" }));
        matchesList.appendChild(empty);
        return;
      }

      matched.forEach((c) => {
        const item = document.createElement("div");
        item.className = "match-item";
        item.setAttribute("role", "button");
        item.setAttribute("tabindex", "0");

        // Highlight matched portion in name if T9 matched
        let displayName = c.name;
        if (q && c.t9.includes(q)) {
          const startIdx = c.t9.indexOf(q);
          const endIdx = startIdx + q.length;
          displayName = `${c.name.slice(0, startIdx)}<span class="match-highlight">${c.name.slice(startIdx, endIdx)}</span>${c.name.slice(endIdx)}`;
        }

        const avatarHtml = c.avatarImg
          ? `<img src="${c.avatarImg}" alt="${c.name}" class="match-avatar-img" />`
          : c.avatar;

        item.innerHTML = `
          <div class="match-item-left">
            <div class="match-avatar">${avatarHtml}</div>
            <div class="match-info">
              <strong>${displayName}</strong>
              <span>${c.number}</span>
            </div>
          </div>
          <button type="button" class="match-dial-btn" aria-label="Call ${c.name}">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92Z"/></svg>
          </button>
        `;

        item.addEventListener("click", () => {
          if (input) input.value = c.number;
          startSimulatedCall(c);
        });

        matchesList.appendChild(item);
      });
    }

    function addDigit(d) {
      if (!input) return;
      playTone(d);
      input.value += d;
      renderMatches(input.value);
    }

    function backspace() {
      if (!input) return;
      input.value = input.value.slice(0, -1);
      renderMatches(input.value);
    }

    keys.forEach((key) => {
      key.addEventListener("click", () => {
        const digit = key.getAttribute("data-digit");
        if (digit) addDigit(digit);
      });
    });

    bsBtn?.addEventListener("click", backspace);

    // Keyboard support for T9 demo when typing numbers
    document.addEventListener("keydown", (e) => {
      const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : "";
      if (activeTag === "input" || activeTag === "textarea") return;

      if (/^[0-9*#]$/.test(e.key)) {
        addDigit(e.key);
      } else if (e.key === "Backspace") {
        backspace();
      }
    });

    // Combo shortcuts
    document.querySelectorAll(".combo-btn").forEach((btn) => {
      btn.addEventListener("click", () => {
        const combo = btn.getAttribute("data-combo");
        if (combo && input) {
          input.value = combo;
          playTone(combo[0] || "1");
          renderMatches(combo);
        }
      });
    });

    const callAvatarImg = document.getElementById("sim-call-avatar-img");
    const callBackdrop = document.getElementById("sim-call-backdrop");

    // Simulated Outgoing Call
    function startSimulatedCall(contact) {
      if (!callModal) return;
      if (callName) callName.textContent = contact.name || "Dialing...";
      if (callNumber) callNumber.textContent = contact.number || input?.value || "Unknown";
      if (callStatusText) callStatusText.textContent = `Calling via SIM ${currentSim}...`;

      const imgSrc = contact.avatarImg || "assets/avatars/sophia.jpg";
      if (callAvatarImg) callAvatarImg.src = imgSrc;
      if (callBackdrop) callBackdrop.style.backgroundImage = `url('${imgSrc}')`;

      callModal.classList.add("is-open");
      callModal.setAttribute("aria-hidden", "false");
    }

    callBtn?.addEventListener("click", () => {
      const num = input?.value || "";
      const match = CONTACTS.find((c) => c.number.includes(num) || c.t9.includes(num));
      startSimulatedCall(match || { name: num ? "Direct Dial" : "Speed Dial #1", number: num || "Voicemail", avatar: "📞" });
    });

    endCallBtn?.addEventListener("click", () => {
      callModal?.classList.remove("is-open");
      callModal?.setAttribute("aria-hidden", "true");
    });

    callModal?.addEventListener("click", (e) => {
      if (e.target === callModal) {
        callModal.classList.remove("is-open");
        callModal.setAttribute("aria-hidden", "true");
      }
    });

    // Initial render
    renderMatches("");
  }

  // ---------------------------------------------------------------------------
  // 5. Feature Category Filters & Mouse Spotlight Glow
  // ---------------------------------------------------------------------------
  // 5. Bento Grid Micro-Interactions (Radial Cursor Spotlight)
  // ---------------------------------------------------------------------------
  function initFeatures() {
    const cards = Array.from(document.querySelectorAll(".bento-card, .feature-card"));

    // Radial Cursor Spotlight
    cards.forEach((card) => {
      card.addEventListener("mousemove", (e) => {
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        card.style.setProperty("--mouse-x", `${x}px`);
        card.style.setProperty("--mouse-y", `${y}px`);
      });
    });
  }

  // ---------------------------------------------------------------------------
  // 6. Screenshots Rail (Drag-to-Scroll, Dots, Lightbox)
  // ---------------------------------------------------------------------------
  function initScreenshots() {
    const rail = document.getElementById("screenshots-rail");
    const prevRail = document.getElementById("rail-prev");
    const nextRail = document.getElementById("rail-next");
    const railDots = document.getElementById("rail-dots");
    const cards = Array.from(document.querySelectorAll(".shot-card"));

    const lightbox = document.getElementById("lightbox");
    const lbImg = document.getElementById("lightbox-img");
    const lbTitle = document.getElementById("lightbox-title");
    const lbCounter = document.getElementById("lightbox-counter");
    const lbPrev = document.getElementById("lightbox-prev");
    const lbNext = document.getElementById("lightbox-next");
    const lbClose = document.getElementById("lightbox-close");
    let activeIdx = 0;

    // Rail buttons
    prevRail?.addEventListener("click", () => rail?.scrollBy({ left: -260, behavior: "smooth" }));
    nextRail?.addEventListener("click", () => rail?.scrollBy({ left: 260, behavior: "smooth" }));

    // Drag-to-scroll implementation
    if (rail) {
      let isDown = false;
      let startX = 0;
      let scrollLeft = 0;
      let hasDragged = false;

      rail.addEventListener("mousedown", (e) => {
        isDown = true;
        hasDragged = false;
        rail.classList.add("is-dragging");
        startX = e.pageX - rail.offsetLeft;
        scrollLeft = rail.scrollLeft;
      });

      rail.addEventListener("mouseleave", () => {
        isDown = false;
        rail.classList.remove("is-dragging");
      });

      rail.addEventListener("mouseup", () => {
        isDown = false;
        rail.classList.remove("is-dragging");
      });

      rail.addEventListener("mousemove", (e) => {
        if (!isDown) return;
        e.preventDefault();
        const x = e.pageX - rail.offsetLeft;
        const walk = (x - startX) * 1.5;
        if (Math.abs(walk) > 6) hasDragged = true;
        rail.scrollLeft = scrollLeft - walk;
      });

      // Pagination Dots
      if (railDots && cards.length) {
        cards.forEach((_, i) => {
          const dot = document.createElement("button");
          dot.type = "button";
          dot.className = `rail-dot ${i === 0 ? "is-active" : ""}`;
          dot.setAttribute("aria-label", `Jump to screenshot ${i + 1}`);
          dot.addEventListener("click", () => {
            cards[i]?.scrollIntoView({ behavior: "smooth", inline: "start", block: "nearest" });
          });
          railDots.appendChild(dot);
        });

        const dots = Array.from(railDots.querySelectorAll(".rail-dot"));
        rail.addEventListener("scroll", () => {
          const max = rail.scrollWidth - rail.clientWidth;
          if (max <= 0) return;
          const ratio = rail.scrollLeft / max;
          const targetIndex = Math.min(cards.length - 1, Math.round(ratio * (cards.length - 1)));
          dots.forEach((d, idx) => d.classList.toggle("is-active", idx === targetIndex));
        }, { passive: true });
      }

      // Card click with drag check
      cards.forEach((card, i) => {
        card.addEventListener("click", (e) => {
          if (hasDragged) {
            e.preventDefault();
            return;
          }
          openLightbox(i);
        });
        card.addEventListener("keydown", (e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            openLightbox(i);
          }
        });
      });
    }

    // Lightbox Logic
    function openLightbox(idx) {
      if (!cards.length || !lightbox || !lbImg) return;
      activeIdx = ((idx % cards.length) + cards.length) % cards.length;
      const card = cards[activeIdx];
      const img = card.querySelector("img");
      lbImg.src = img.getAttribute("src");
      lbImg.alt = img.getAttribute("alt") || "";
      if (lbTitle) {
        const titleSpan = card.querySelector("figcaption span:last-child");
        lbTitle.textContent = titleSpan ? titleSpan.textContent : (card.querySelector("figcaption")?.textContent || "");
      }
      if (lbCounter) lbCounter.textContent = `${activeIdx + 1} / ${cards.length}`;

      lightbox.classList.add("open");
      lightbox.setAttribute("aria-hidden", "false");
      document.body.style.overflow = "hidden";
    }

    function closeLightbox() {
      if (!lightbox) return;
      lightbox.classList.remove("open");
      lightbox.setAttribute("aria-hidden", "true");
      document.body.style.overflow = "";
    }

    document.querySelectorAll("[data-open-shot]").forEach((el) => {
      el.addEventListener("click", () => {
        const file = el.getAttribute("data-open-shot");
        const idx = cards.findIndex((c) => c.querySelector("img")?.getAttribute("src")?.endsWith(file));
        if (idx >= 0) openLightbox(idx);
      });
    });

    lbPrev?.addEventListener("click", () => openLightbox(activeIdx - 1));
    lbNext?.addEventListener("click", () => openLightbox(activeIdx + 1));
    lbClose?.addEventListener("click", closeLightbox);
    lightbox?.addEventListener("click", (e) => {
      if (e.target === lightbox) closeLightbox();
    });

    // Touch Swipe Gesture for Lightbox on mobile
    let touchStartX = 0;
    lightbox?.addEventListener("touchstart", (e) => {
      touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });

    lightbox?.addEventListener("touchend", (e) => {
      const touchEndX = e.changedTouches[0].screenX;
      const diff = touchEndX - touchStartX;
      if (Math.abs(diff) > 40) {
        if (diff > 0) openLightbox(activeIdx - 1);
        else openLightbox(activeIdx + 1);
      }
    }, { passive: true });

    document.addEventListener("keydown", (e) => {
      if (!lightbox?.classList.contains("open")) return;
      if (e.key === "Escape") closeLightbox();
      if (e.key === "ArrowLeft") openLightbox(activeIdx - 1);
      if (e.key === "ArrowRight") openLightbox(activeIdx + 1);
    });
  }

  // ---------------------------------------------------------------------------
  // 7. Back-to-Top Floating Button
  // ---------------------------------------------------------------------------
  function initBackToTop() {
    const btn = document.getElementById("back-to-top");
    if (!btn) return;

    window.addEventListener("scroll", () => {
      btn.classList.toggle("show", window.scrollY > 350);
    }, { passive: true });

    btn.addEventListener("click", () => {
      window.scrollTo({ top: 0, behavior: "smooth" });
    });
  }

  // ---------------------------------------------------------------------------
  // Lifecycle Initialization
  // ---------------------------------------------------------------------------
  document.addEventListener("DOMContentLoaded", () => {
    initThemeAndAccent();
    initNav();
    initHeroShowcase();
    initDialerDemo();
    initFeatures();
    initScreenshots();
    initBackToTop();
  });
})();
