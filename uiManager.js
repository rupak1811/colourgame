// ...existing code...

// Add a function to load and apply the UI from back_bg.json
async function loadAndApplyUI() {
    const response = await fetch('back_bg.json');
    const uiData = await response.json();

    // Clear existing UI
    clearCurrentUI();

    // Create UI elements based on JSON
    uiData.elements.forEach(element => {
        const uiElement = createUIElement(element);
        document.body.appendChild(uiElement);

        // Apply animations if present
        if (element.animation) {
            applyAnimation(uiElement, element.animation);
        }
    });
}

function createUIElement(element) {
    const el = document.createElement('div');
    el.style.position = 'absolute';
    el.style.left = element.x + 'px';
    el.style.top = element.y + 'px';
    el.style.width = element.width + 'px';
    el.style.height = element.height + 'px';
    if (element.image) {
        el.style.backgroundImage = `url(${element.image})`;
        el.style.backgroundSize = 'cover';
    }
    // ...add more properties as needed...
    return el;
}

function applyAnimation(el, animation) {
    // Example: simple fade-in animation
    if (animation.type === 'fade') {
        el.style.opacity = 0;
        el.style.transition = `opacity ${animation.duration}ms`;
        setTimeout(() => {
            el.style.opacity = 1;
        }, 10);
    }
    // ...handle other animation types...
}

function clearCurrentUI() {
    // Remove all dynamically added UI elements
    // ...implementation...
}

// Call this function when you want to update the UI
// loadAndApplyUI();

// ...existing code...
