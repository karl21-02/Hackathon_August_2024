window.onload = function() {
    document.getElementById('memberForm').addEventListener('submit', function(event) {
        event.preventDefault();
    
        // Form data collection
        const familyName = document.getElementById('familyName').value;
        const givenName = document.getElementById('givenName').value;
        const birth = document.getElementById('birth').value;
        const phoneNumber = document.getElementById('phoneNumber').value;
        const address = document.getElementById('address').value;
        const gender = document.getElementById('gender').value;
        const height = parseInt(document.getElementById('height').value);
        const weight = parseInt(document.getElementById('weight').value);
        const bloodType = document.getElementById('bloodType').value;
        const rhNegative = document.getElementById('rhNegative').checked;
    
        // JSON object creation
        const memberData = {
            familyName: familyName,
            givenName: givenName,
            birth: birth,
            phoneNumber: phoneNumber,
            address: address,
            gender: gender,
            height: height,
            weight: weight,
            bloodType: bloodType,
            rhNegative: rhNegative
        };
    
        // Sending JSON data to the server
        fetch('/member/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(memberData)
        })
        .then(response => {
            if (response.ok) {
                alert('Member saved successfully!');
            } else {
                alert('Failed to save member.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('An error occurred while saving the member.');
        });
    });    
}