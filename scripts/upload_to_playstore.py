"""
Upload AAB to Google Play Console using official Google API
Requires: pip install google-api-python-client google-auth-httplib2
"""
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
import sys
import argparse

SCOPES = ['https://www.googleapis.com/auth/androidpublisher']
PACKAGE_NAME = 'com.mobileorienteering'

def upload_to_play_store(credentials_file, aab_file, track='internal', release_notes=None):
    """Upload AAB to Play Store"""
    print(f"Uploading {aab_file} to {track} track...")
    
    credentials = service_account.Credentials.from_service_account_file(
        credentials_file, scopes=SCOPES)
    
    service = build('androidpublisher', 'v3', credentials=credentials)
    
    # Create an edit
    edit_request = service.edits().insert(packageName=PACKAGE_NAME)
    edit = edit_request.execute()
    edit_id = edit['id']
    print(f"Created edit: {edit_id}")
    
    try:
        # Upload the AAB
        print("Uploading AAB...")
        aab_response = service.edits().bundles().upload(
            editId=edit_id,
            packageName=PACKAGE_NAME,
            media_body=MediaFileUpload(aab_file, mimetype='application/octet-stream')
        ).execute()
        
        version_code = aab_response['versionCode']
        print(f'Uploaded version code: {version_code}')
        
        # Prepare release
        release_body = {
            'versionCodes': [version_code],
            'status': 'completed'
        }
        
        # Add release notes if provided
        if release_notes:
            release_body['releaseNotes'] = [{
                'language': 'en-US',
                'text': release_notes
            }]
        
        # Assign to track
        print(f"Assigning to {track} track...")
        track_response = service.edits().tracks().update(
            editId=edit_id,
            track=track,
            packageName=PACKAGE_NAME,
            body={'releases': [release_body]}
        ).execute()
        
        print(f'Assigned to track: {track}')
        
        # Commit the edit
        print("Committing changes...")
        commit_request = service.edits().commit(
            editId=edit_id,
            packageName=PACKAGE_NAME
        ).execute()
        
        print(f'Successfully published to {track} track!')
        return True
        
    except Exception as e:
        print(f'Error: {e}')
        return False

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Upload AAB to Google Play Console')
    parser.add_argument('credentials', help='Path to service account JSON')
    parser.add_argument('aab', help='Path to AAB file')
    parser.add_argument('--track', default='internal', 
                       choices=['internal', 'alpha', 'beta', 'production'],
                       help='Release track (default: internal)')
    parser.add_argument('--notes', help='Release notes')
    
    args = parser.parse_args()
    
    success = upload_to_play_store(args.credentials, args.aab, args.track, args.notes)
    sys.exit(0 if success else 1)
