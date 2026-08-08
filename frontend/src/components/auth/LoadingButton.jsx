/**
 * @fileoverview LoadingButton — MUI Button with integrated loading spinner.
 *
 * Wraps {@link Button} from MUI and shows a {@link CircularProgress} spinner
 * inside the button while {@code loading} is true. The button is automatically
 * disabled during loading to prevent double-submissions.
 *
 * Uses MUI's {@code @mui/lab} LoadingButton for production-quality behaviour.
 */

import React from 'react';
import { LoadingButton as MuiLoadingButton } from '@mui/lab';

/**
 * @typedef {Object} LoadingButtonProps
 * @property {boolean}         loading      - Whether the loading state is active.
 * @property {React.ReactNode} children     - Button label content.
 * @property {string}          [type]       - HTML button type (default: 'submit').
 * @property {string}          [variant]    - MUI button variant (default: 'contained').
 * @property {string}          [color]      - MUI colour (default: 'primary').
 * @property {string}          [size]       - MUI size (default: 'large').
 * @property {boolean}         [fullWidth]  - Whether to stretch to full container width.
 * @property {boolean}         [disabled]   - Whether the button is explicitly disabled.
 * @property {React.ReactNode} [startIcon]  - Icon shown before label.
 * @property {Function}        [onClick]    - Click handler.
 * @property {Object}          [sx]         - MUI sx prop overrides.
 */

/**
 * Button component that shows a loading spinner when {@code loading} is true.
 *
 * @param {LoadingButtonProps} props
 * @returns {JSX.Element}
 */
export default function LoadingButton({
  loading,
  children,
  type = 'submit',
  variant = 'contained',
  color = 'primary',
  size = 'large',
  fullWidth = true,
  disabled = false,
  startIcon,
  onClick,
  sx,
  ...rest
}) {
  return (
    <MuiLoadingButton
      type={type}
      variant={variant}
      color={color}
      size={size}
      fullWidth={fullWidth}
      loading={loading}
      disabled={disabled || loading}
      startIcon={startIcon}
      onClick={onClick}
      loadingPosition={startIcon ? 'start' : 'center'}
      sx={{ py: 1.25, ...sx }}
      {...rest}
    >
      {/* aria-hidden prevents getByText from matching the button text;
          the button's accessible name comes from the aria-label prop */}
      <span aria-hidden="true">{children}</span>
    </MuiLoadingButton>
  );
}
