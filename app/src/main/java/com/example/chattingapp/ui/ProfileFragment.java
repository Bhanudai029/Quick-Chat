package com.example.chattingapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.chattingapp.R;
import com.example.chattingapp.databinding.FragmentProfileBinding;
import com.example.chattingapp.utils.MediaValidator;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleProfileImageSelected(imageUri);
                    }
                }
            }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupUI();
        loadCurrentProfile();

        return root;
    }
    
    private void setupUI() {
        // Click on profile image card to change
        binding.profileImageCard.setOnClickListener(v -> {
            openImagePicker();
        });
        
        // Also allow click on the image itself
        binding.profileImage.setOnClickListener(v -> {
            openImagePicker();
        });
        
        // Save button
        binding.btnSave.setOnClickListener(v -> {
            saveProfile();
        });

        // Dynamic avatar update and real-time validation
        binding.editName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (selectedImageUri == null) {
                    binding.profileImage.setImageDrawable(new com.example.chattingapp.utils.AvatarDrawable(s.toString()));
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                validateAndUpdateButton();
            }
        });
        
        // Initial validation state
        validateAndUpdateButton();
    }
    
    private void validateAndUpdateButton() {
        String name = binding.editName.getText().toString().trim();
        boolean hasName = !name.isEmpty();
        
        binding.btnSave.setEnabled(hasName);
        binding.btnSave.setAlpha(hasName ? 1.0f : 0.5f);
    }
    
    private void loadCurrentProfile() {
        com.example.chattingapp.utils.UserManager userManager = new com.example.chattingapp.utils.UserManager(requireContext());
        String savedName = userManager.getUserName();
        if (savedName != null && !savedName.isEmpty()) {
            binding.editName.setText(savedName);
            if (selectedImageUri == null) {
                binding.profileImage.setImageDrawable(new com.example.chattingapp.utils.AvatarDrawable(savedName));
            }
        } else {
             if (selectedImageUri == null) {
                binding.profileImage.setImageDrawable(new com.example.chattingapp.utils.AvatarDrawable("?"));
            }
        }
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }
    
    private void handleProfileImageSelected(Uri imageUri) {
        String mimeType = getContext().getContentResolver().getType(imageUri);
        
        if (!MediaValidator.isImage(mimeType)) {
            Toast.makeText(getContext(), "Please select a JPEG or PNG image", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectedImageUri = imageUri;
        
        // Show selected image with circular crop
        Glide.with(this)
            .load(imageUri)
            .centerCrop()
            .into(binding.profileImage);
    }
    
    private void saveProfile() {
        String name = binding.editName.getText().toString().trim();
        binding.nameInputLayout.setError(null); // Clear previous error
        
        if (name.isEmpty()) {
            binding.nameInputLayout.setError("Name is required");
            return;
        }

        if (name.length() > 30) {
            binding.nameInputLayout.setError("Name cannot exceed 30 characters");
            return;
        }

        // Check for symbols/numbers (letters and spaces only)
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            binding.nameInputLayout.setError("Name must contain only letters");
            return;
        }

        // Check for spam (repeating characters 4 or more times, e.g., "ssss")
        if (hasRepeatingChars(name, 4)) {
            binding.nameInputLayout.setError("Please enter a valid name");
            return;
        }
        
        // Save locally
        com.example.chattingapp.utils.UserManager userManager = new com.example.chattingapp.utils.UserManager(requireContext());
        userManager.saveUserName(name);
        
        // TODO: Upload image to Supabase Storage if changed
        // TODO: Update user profile in Supabase
        
        Toast.makeText(getContext(), "Profile saved!", Toast.LENGTH_SHORT).show();
    }

    private boolean hasRepeatingChars(String str, int limit) {
        if (str == null || str.length() < limit) return false;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(.)\\1{" + (limit - 1) + ",}");
        return p.matcher(str).find();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
