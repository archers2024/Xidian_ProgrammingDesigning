import {it,expect} from 'vitest'
import {formatTime,safeUrl} from '../src/utils/display'
it('keeps the specified business time independent of browser parsing',()=>{expect(formatTime('2026-11-16T12:30:00')).toBe('2026-11-16 12:30:00')})
it('rejects script and credential-bearing external addresses',()=>{expect(safeUrl('javascript:alert(1)')).toBe('');expect(safeUrl('https://user:pass@example.com')).toBe('')})
