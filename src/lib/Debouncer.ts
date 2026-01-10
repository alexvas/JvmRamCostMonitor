const DEBOUNCE_DELAY_MS = 16; // примерно один кадр

export class Debouncer {
    private pendingUpdateCounter = 0;
    callback: () => void;

    constructor(callback: () => void) {
        this.callback = callback;
    }

    public debounce() {
        this.pendingUpdateCounter++;
        setTimeout(
            () => {
                this.pendingUpdateCounter--;
                if (this.pendingUpdateCounter <= 0) {
                    this.callback();
                }
            },
            DEBOUNCE_DELAY_MS
        );
    }

}